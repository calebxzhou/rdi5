package calebxzhou.rdi.prox

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.util.ReferenceCountUtil

/**
 * Enhanced handler for frontend connections with simplified port-based routing
 * Client sends 2 bytes (big-endian port) at connection start, then all data is forwarded
 */
class DynamicProxyFrontendHandler(
    private val defaultBackendHost: String,
    private val defaultBackendPort: Int,
    private val backendGroup: EventLoopGroup
) : ChannelInboundHandlerAdapter() {

    private var backendChannel: Channel? = null
    private var currentBackendHost: String = defaultBackendHost
    private var currentBackendPort: Int = defaultBackendPort
    private val pendingBuffer = mutableListOf<Any>()
    private var handshakeReceived = false

    override fun channelActive(ctx: ChannelHandlerContext) {
        lgr.info { "Client connected, waiting for Minecraft handshake" }
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (!handshakeReceived) {
            val buffer = msg as ByteBuf
            try {
                handleHandshake(ctx, buffer)
            } catch (e: Exception) {
                lgr.error(e) { "Error parsing handshake" }
                ctx.channel().close()
            } finally {
                ReferenceCountUtil.release(msg)
            }
            return
        }

        // After handshake, forward all data normally
        forwardToBackend(ctx, msg)
    }

    private fun handleHandshake(ctx: ChannelHandlerContext, buffer: ByteBuf) {
        // Buffer contains [Length + PacketID + Protocol + Host + Port + State]
        // We need to parse this non-destructively to extract the port
        val parser = buffer.slice()

        // 1. Skip Packet Length (VarInt)
        readVarInt(parser)

        // 2. Read Packet ID (VarInt) - must be 0x00 for Handshake
        val packetId = readVarInt(parser)

        if (packetId == 0) {
            // 3. Skip Protocol Version (VarInt)
            readVarInt(parser)

            // 4. Skip Hostname (String = VarInt Len + Bytes)
            val hostLen = readVarInt(parser)
            parser.skipBytes(hostLen)

            // 5. Read Port (UShort)
            val port = parser.readUnsignedShort()

            handshakeReceived = true
            lgr.info { "Handshake received: Port $port" }

            // Determine backend based on port
            if (port in 50000..59999 || port == 25565) {
                lgr.info { "Connecting to backend 127.0.0.1:$port" }
                currentBackendHost = "127.0.0.1"
                currentBackendPort = port

                connectToBackend(ctx)

                // Forward the handshake packet
                forwardToBackend(ctx, buffer.retain())

                // Remove the frame decoder so subsequent encrypted/compressed packets flow raw
                ctx.pipeline().remove(MinecraftFrameDecoder::class.java)
                lgr.info { "Removed MinecraftFrameDecoder, switching to raw forwarding" }
            } else {
                lgr.info { "Invalid port $port (must be 50000-59999), closing connection" }
                ctx.channel().close()
            }
        } else {
            lgr.info { "Expected Handshake (0x00) but got $packetId, closing" }
            ctx.channel().close()
        }
    }


    private fun readVarInt(buffer: ByteBuf): Int {
        var value = 0
        var position = 0
        var currentByte: Byte

        while (true) {
            currentByte = buffer.readByte()
            value = value or ((currentByte.toInt() and 0x7F) shl position)

            if ((currentByte.toInt() and 0x80) == 0) break

            position += 7

            if (position >= 32) throw RuntimeException("VarInt is too big")
        }

        return value
    }


    private fun connectToBackend(ctx: ChannelHandlerContext) {
        val frontendChannel = ctx.channel()

        // Don't attempt backend connection if frontend is already closed
        if (!frontendChannel.isActive) {
            lgr.info { "Frontend channel closed, aborting backend connection" }
            return
        }

        // Create connection to backend server
        val bootstrap = Bootstrap()
        bootstrap.group(backendGroup)
            .channel(NioSocketChannel::class.java)
            .option(ChannelOption.AUTO_READ, true)
            .option(ChannelOption.TCP_NODELAY, true)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(ProxyBackendHandler(frontendChannel))

        val future = bootstrap.connect(currentBackendHost, currentBackendPort)
        backendChannel = future.channel()

        future.addListener { connectFuture ->
            // Check if frontend is still active before proceeding
            if (!frontendChannel.isActive) {
                lgr.info { "Frontend disconnected during backend connection, closing backend" }
                if (connectFuture.isSuccess) {
                    future.channel().close()
                }
                // Release any pending buffered data
                pendingBuffer.forEach { bufferedMsg ->
                    ReferenceCountUtil.release(bufferedMsg)
                }
                pendingBuffer.clear()
                return@addListener
            }

            if (connectFuture.isSuccess) {
                lgr.info { "Connected to backend: $currentBackendHost:$currentBackendPort" }

                // Send any pending buffered data immediately
                if (pendingBuffer.isNotEmpty()) {
                    val compositeBuf = ctx.alloc().compositeBuffer(pendingBuffer.size)
                    pendingBuffer.forEach { bufferedMsg ->
                        if (bufferedMsg is ByteBuf) {
                            compositeBuf.addComponent(true, bufferedMsg)
                        }
                    }
                    pendingBuffer.clear()
                    if (compositeBuf.isReadable) {
                        future.channel().writeAndFlush(compositeBuf)
                    } else {
                        compositeBuf.release()
                    }
                }
            } else {
                // Connection failed, close frontend
                lgr.info { "Failed to connect to backend: ${connectFuture.cause()?.message}" }

                // Release any pending buffered data
                pendingBuffer.forEach { bufferedMsg ->
                    ReferenceCountUtil.release(bufferedMsg)
                }
                pendingBuffer.clear()

                frontendChannel.close()
            }
        }
    }

    private fun forwardToBackend(ctx: ChannelHandlerContext, msg: Any) {
        val frontendChannel = ctx.channel()

        // Check if frontend is still active
        if (!frontendChannel.isActive) {
            ReferenceCountUtil.release(msg)
            return
        }

        if (backendChannel?.isActive == true) {
            // Forward data to backend immediately with flush
            backendChannel?.writeAndFlush(msg)?.addListener { future ->
                if (!future.isSuccess) {
                    // Write failed, close connection
                    lgr.info { "Failed to write to backend: ${future.cause()?.message}" }
                    ctx.channel().close()
                }
            }
        } else {
            // Backend not available or not yet chosen: buffer the message
            pendingBuffer.add(msg)
        }
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        if (backendChannel?.isActive == true) {
            closeOnFlush(backendChannel!!)
        }
        // Release any pending buffered data
        pendingBuffer.forEach { bufferedMsg ->
            ReferenceCountUtil.release(bufferedMsg)
        }
        pendingBuffer.clear()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        lgr.info { "Frontend exception: ${cause.message}" }
        closeOnFlush(ctx.channel())
    }

    private fun closeOnFlush(ch: Channel) {
        if (ch.isActive) {
            ch.writeAndFlush(Unpooled.EMPTY_BUFFER)
                .addListener(ChannelFutureListener.CLOSE)
        }
    }
}