package calebxzhou.rdi.prox

import io.netty.bootstrap.Bootstrap
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.util.CharsetUtil

/**
 * TCP Reverse Proxy using Netty with dynamic backend switching
 * Routes incoming connections to backend servers based on binary control packets
 *
 * Binary packet format: 0x01 02 03 04 AA BB CC DD + 2 bytes port (big-endian)
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
            lgr.info { "Send binary control packet (0x01020304AABBCCDD + port) to redirect to 127.0.0.1:<port>" }

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
        ch.pipeline().addLast(
            // 启用包数据的日志记录
            // LoggingHandler(LogLevel.INFO),
            DynamicProxyFrontendHandler(defaultBackendHost, defaultBackendPort, backendGroup)
        )
    }
}

/**
 * Enhanced handler for frontend connections that supports dynamic backend switching
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

    override fun channelActive(ctx: ChannelHandlerContext) {
        // Connect to default backend immediately
        lgr.info { "Client connected, connecting to default backend..." }
        connectToBackend(ctx)
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        // Always check for control packets, regardless of when they arrive
        val controlResult = checkForControlPacket(ctx, msg)
        if (!controlResult) {
            // Not a control packet, forward to backend
            forwardToBackend(ctx, msg)
        }
    }

    private fun checkForControlPacket(ctx: ChannelHandlerContext, msg: Any): Boolean {
        val buffer = msg as ByteBuf

        // Check if this is a binary port switching command
        if (buffer.readableBytes() == 10) { // 8 bytes header + 2 bytes port
            // Mark the reader index to reset if it's not a control packet
            buffer.markReaderIndex()

            // Check for the magic header: 0x01 02 03 04 AA BB CC DD
            val b1 = buffer.readByte()
            val b2 = buffer.readByte()
            val b3 = buffer.readByte()
            val b4 = buffer.readByte()
            val b5 = buffer.readByte()
            val b6 = buffer.readByte()
            val b7 = buffer.readByte()
            val b8 = buffer.readByte()

            if (b1 == 0x01.toByte() && b2 == 0x02.toByte() && b3 == 0x03.toByte() && b4 == 0x04.toByte() &&
                b5 == 0xAA.toByte() && b6 == 0xBB.toByte() && b7 == 0xCC.toByte() && b8 == 0xDD.toByte()) {

                // Read the next 2 bytes as port number (big-endian)
                val newPort = buffer.readUnsignedShort()

                if (newPort in 1..65535) {
                    // Check if we need to switch backends
                    if (newPort != currentBackendPort) {
                        lgr.info { "Binary port switching: changing backend from $currentBackendHost:$currentBackendPort to 127.0.0.1:$newPort" }
                        switchBackend(ctx, "127.0.0.1", newPort)
                    } else {
                        lgr.info { "Binary control packet received but already connected to port $newPort" }
                    }

                    // Check if there's remaining data after the control packet
                    if (buffer.readableBytes() > 0) {
                        // Create a new buffer with the remaining data and forward it
                        val remainingData = buffer.slice()
                        remainingData.retain() // Increase ref count for the slice
                        forwardToBackend(ctx, remainingData)
                    }

                    // Release the original control packet
                    io.netty.util.ReferenceCountUtil.release(msg)
                    return true // Control packet consumed
                } else {
                    lgr.info { "Invalid binary port number: $newPort, ignoring control packet" }
                    // Reset reader index to treat as regular data
                    buffer.resetReaderIndex()
                }
            } else {
                // Not a binary control packet, reset reader index
                buffer.resetReaderIndex()
            }
        }

        return false // Not a control packet
    }

    private fun switchBackend(ctx: ChannelHandlerContext, newHost: String, newPort: Int) {
        // Close existing backend connection if active
        if (backendChannel?.isActive == true) {
            lgr.info { "Closing existing backend connection to $currentBackendHost:$currentBackendPort" }
            backendChannel?.close()
        }

        // Update backend details
        currentBackendHost = newHost
        currentBackendPort = newPort

        // Connect to new backend
        connectToBackend(ctx)
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
            .option(ChannelOption.AUTO_READ, false)
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
                // Connection established, start reading from frontend
                frontendChannel.config().isAutoRead = true
                lgr.info { "Connected to backend: $currentBackendHost:$currentBackendPort" }

                // Send any pending buffered data
                pendingBuffer.forEach { bufferedMsg ->
                    forwardToBackend(ctx, bufferedMsg)
                }
                pendingBuffer.clear()
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
            // Forward data to backend
            backendChannel?.writeAndFlush(msg)?.addListener { future ->
                if (future.isSuccess) {
                    // Continue reading from frontend only if still active
                    if (frontendChannel.isActive) {
                        ctx.channel().read()
                    }
                } else {
                    // Write failed, close connection
                    ctx.channel().close()
                }
            }
        } else {
            // Backend not available, buffer the message if we're still connecting
            // Check if we have a backend channel that's connecting
            if (backendChannel != null && !backendChannel!!.isActive) {
                // Still connecting, buffer the message
                pendingBuffer.add(msg)
            } else {
                // No backend connection attempt or already failed, release and close
                io.netty.util.ReferenceCountUtil.release(msg)
                ctx.close()
            }
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
        // Start reading from backend
        ctx.read()
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        // Forward data to frontend
        frontendChannel.writeAndFlush(msg).addListener { future ->
            if (future.isSuccess) {
                // Continue reading from backend
                ctx.channel().read()
            } else {
                // Write failed, close connection
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
