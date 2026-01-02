package calebxzhou.rdi.prox

import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.MessageToByteEncoder
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.util.CharsetUtil

/**
 * TCP Reverse Proxy using Netty with dynamic backend switching
 * Routes incoming connections to backend servers based on port in first 2 bytes
 *
 * Connection flow: Client sends 2 bytes (big-endian port), then forwards all data
 */
class TcpReverseProxy {

    private val bossGroup = NioEventLoopGroup(1)
    private val workerGroup = NioEventLoopGroup()
    private val backendGroup = NioEventLoopGroup()
    private var serverChannel: Channel? = null

    /**
     * Start the reverse proxy
     * @param bindHost Host to bind the proxy server to
     * @param bindPort Port to bind the proxy server to
     * @param defaultBackendHost Default backend server host
     * @param defaultBackendPort Default backend server port
     */
    fun start(bindHost: String, bindPort: Int, defaultBackendHost: String, defaultBackendPort: Int) {
        try {
            val bootstrap = ServerBootstrap()
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .option(ChannelOption.SO_BACKLOG, 100)
                .handler(LoggingHandler(LogLevel.INFO))
                .childHandler(ProxyServerInitializer(defaultBackendHost, defaultBackendPort, backendGroup))

            // Bind and start to accept incoming connections
            val future = bootstrap.bind(bindHost, bindPort).sync()
            serverChannel = future.channel()

            lgr.info { "TCP Reverse Proxy started on $bindHost:$bindPort" }
            lgr.info { "Default backend: $defaultBackendHost:$defaultBackendPort" }
            lgr.info { "Client protocol: Send 2 bytes (big-endian port 50000-59999) at start, then forward data" }

            // Wait until the server socket is closed
            future.channel().closeFuture().sync()
        } finally {
            shutdown()
        }
    }

    /**
     * Shutdown the proxy server gracefully
     */
    fun shutdown() {
        serverChannel?.close()
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
        backendGroup.shutdownGracefully()
    }
}

/**
 * Channel initializer for incoming client connections
 */
class ProxyServerInitializer(
    private val defaultBackendHost: String,
    private val defaultBackendPort: Int,
    private val backendGroup: EventLoopGroup
) : ChannelInitializer<SocketChannel>() {

    override fun initChannel(ch: SocketChannel) {
        ch.config().setOption(ChannelOption.TCP_NODELAY, true)
        ch.config().setOption(ChannelOption.SO_KEEPALIVE, true)
        if(Const.DEBUG){
            //记录包的内容
            ch.pipeline().addLast(LoggingHandler(LogLevel.INFO))
        }
        ch.pipeline().addLast(
            // Port extraction happens first, then Minecraft framing is added dynamically
            DynamicProxyFrontendHandler(defaultBackendHost, defaultBackendPort, backendGroup)
        )
    }
}

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
    private var portReceived = false

    override fun channelActive(ctx: ChannelHandlerContext) {
        lgr.info { "Client connected, waiting for 2-byte port" }
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        // First message must be exactly 2 bytes containing the target port
        if (!portReceived) {
            val buffer = msg as ByteBuf
            if (buffer.readableBytes() >= 2) {
                val port = buffer.readUnsignedShort()
                portReceived = true
                
                if (port in 50000..59999 || port == 25565) {
                    lgr.info { "Port received: $port, connecting to backend 127.0.0.1:$port" }
                    currentBackendHost = "127.0.0.1"
                    currentBackendPort = port
                    
                    // Now add Minecraft frame decoder AFTER port is extracted
                    ctx.pipeline().addBefore(ctx.name(), "minecraftDecoder", MinecraftFrameDecoder())
                    lgr.info { "Added Minecraft frame decoder to pipeline" }
                    
                    connectToBackend(ctx)
                    
                    // If there's remaining data in this packet, forward it
                    if (buffer.readableBytes() > 0) {
                        val remainingData = buffer.readRetainedSlice(buffer.readableBytes())
                        // Re-fire the remaining data through the pipeline so it gets decoded
                        ctx.fireChannelRead(remainingData)
                        io.netty.util.ReferenceCountUtil.release(msg)
                        return
                    }
                } else {
                    lgr.info { "Invalid port $port (must be 50000-59999), closing connection" }
                    ctx.channel().close()
                }
                
                io.netty.util.ReferenceCountUtil.release(msg)
            } else {
                lgr.info { "First packet too small (${buffer.readableBytes()} bytes), expected at least 2" }
                io.netty.util.ReferenceCountUtil.release(msg)
                ctx.channel().close()
            }
            return
        }

        // After port is received, forward all data normally
        forwardToBackend(ctx, msg)
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
                    io.netty.util.ReferenceCountUtil.release(bufferedMsg)
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
                    io.netty.util.ReferenceCountUtil.release(bufferedMsg)
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
            io.netty.util.ReferenceCountUtil.release(msg)
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
            io.netty.util.ReferenceCountUtil.release(bufferedMsg)
        }
        pendingBuffer.clear()
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        lgr.info { "Frontend exception: ${cause.message}" }
        closeOnFlush(ctx.channel())
    }

    private fun closeOnFlush(ch: Channel) {
        if (ch.isActive) {
            ch.writeAndFlush(io.netty.buffer.Unpooled.EMPTY_BUFFER)
                .addListener(ChannelFutureListener.CLOSE)
        }
    }
}

/**
 * Handler for backend (server-facing) connections
 */
class ProxyBackendHandler(
    private val frontendChannel: Channel
) : ChannelInboundHandlerAdapter() {

    override fun channelActive(ctx: ChannelHandlerContext) {
        // Backend is ready, connection established
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        // Forward data to frontend immediately with flush
        frontendChannel.writeAndFlush(msg).addListener { future ->
            if (!future.isSuccess) {
                // Write failed, close connection
                lgr.info { "Failed to write to frontend: ${future.cause()?.message}" }
                ctx.channel().close()
            }
        }
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        closeOnFlush(frontendChannel)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        lgr.info { "Backend exception: ${cause.message}" }
        closeOnFlush(ctx.channel())
    }

    private fun closeOnFlush(ch: Channel) {
        if (ch.isActive) {
            ch.writeAndFlush(io.netty.buffer.Unpooled.EMPTY_BUFFER)
                .addListener(ChannelFutureListener.CLOSE)
        }
    }
}

// Minecraft VarInt frame decoder - decodes packet length from VarInt prefix
class MinecraftFrameDecoder : ByteToMessageDecoder() {
    override fun decode(ctx: ChannelHandlerContext, buffer: ByteBuf, out: MutableList<Any>) {
        val frameStartIndex = buffer.readerIndex()
        buffer.markReaderIndex()

        // Read VarInt length
        val length = readVarInt(buffer)
        if (length == -1) {
            buffer.readerIndex(frameStartIndex)
            return // Not enough bytes to read VarInt
        }

        val frameHeaderEndIndex = buffer.readerIndex()

        // Check if full packet is available
        if (buffer.readableBytes() < length) {
            buffer.readerIndex(frameStartIndex)
            return // Wait for more data
        }

        val totalFrameLength = frameHeaderEndIndex - frameStartIndex + length
        buffer.readerIndex(frameStartIndex)
        out.add(buffer.readRetainedSlice(totalFrameLength))
    }
    
    private fun readVarInt(buffer: ByteBuf): Int {
        var value = 0
        var position = 0
        
        while (true) {
            if (!buffer.isReadable) return -1
            
            val currentByte = buffer.readByte().toInt()
            value = value or ((currentByte and 0x7F) shl position)
            
            if ((currentByte and 0x80) == 0) break
            
            position += 7
            if (position >= 21) throw RuntimeException("VarInt too big")
        }
        
        return value
    }
}

// Minecraft VarInt frame encoder - prepends packet length as VarInt
class MinecraftFrameEncoder : MessageToByteEncoder<ByteBuf>() {
    override fun encode(ctx: ChannelHandlerContext, msg: ByteBuf, out: ByteBuf) {
        val length = msg.readableBytes()
        writeVarInt(length, out)
        out.writeBytes(msg)
    }
    
    private fun writeVarInt(value: Int, buffer: ByteBuf) {
        var v = value
        while (true) {
            if ((v and 0x7F.inv()) == 0) {
                buffer.writeByte(v)
                return
            }
            buffer.writeByte((v and 0x7F) or 0x80)
            v = v ushr 7
        }
    }
}
