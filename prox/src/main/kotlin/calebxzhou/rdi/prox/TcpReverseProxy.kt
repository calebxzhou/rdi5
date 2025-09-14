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
           //  LoggingHandler(LogLevel.INFO),
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
    private var controlBuf: ByteBuf? = null

    // Parse exactly 10-byte control packet: magic(8) + port(2, big-endian).
    // On success, the 10 bytes are consumed from the buffer; returns target port.
    // On failure, reader index is reset and null is returned.
    private fun parseControlPort(buffer: ByteBuf): Int? {
        if (buffer.readableBytes() != 10) return null
        buffer.markReaderIndex()
        val b1 = buffer.readByte()
        val b2 = buffer.readByte()
        val b3 = buffer.readByte()
        val b4 = buffer.readByte()
        val b5 = buffer.readByte()
        val b6 = buffer.readByte()
        val b7 = buffer.readByte()
        val b8 = buffer.readByte()
        val isMagic =
            (b1 == 0x01.toByte() && b2 == 0x02.toByte() && b3 == 0x03.toByte() && b4 == 0x04.toByte() &&
             b5 == 0xAA.toByte() && b6 == 0xBB.toByte() && b7 == 0xCC.toByte() && b8 == 0xDD.toByte())
        if (!isMagic) {
            buffer.resetReaderIndex()
            return null
        }
        val port = buffer.readUnsignedShort()
        if (port !in 50000..59999) {
            buffer.resetReaderIndex()
            return null
        }
        return port
    }

    // Find the start index of the 8-byte magic header within the buffer, or -1 if not found.
    private fun findMagicIndex(buf: ByteBuf): Int {
        val start = buf.readerIndex()
        val end = buf.readerIndex() + buf.readableBytes() - 8
        if (end < start) return -1
        val m = byteArrayOf(0x01,0x02,0x03,0x04,0xAA.toByte(),0xBB.toByte(),0xCC.toByte(),0xDD.toByte())
        var i = start
        while (i <= end) {
            var j = 0
            while (j < 8 && buf.getByte(i + j) == m[j]) j++
            if (j == 8) return i
            i++
        }
        return -1
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        // Do NOT connect immediately. Wait for client to send control packet indicating target port.
        lgr.info { "Client connected, waiting for control packet (0x01020304AABBCCDD + port)" }
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        // Before backend selection, we must accumulate until we get at least the 10-byte control header
        if (backendChannel == null) {
            val incoming = msg as ByteBuf
            val cb = controlBuf ?: run {
                controlBuf = ctx.alloc().buffer(16)
                controlBuf!!
            }
            cb.writeBytes(incoming)
            io.netty.util.ReferenceCountUtil.release(incoming)

            if (cb.readableBytes() >= 10) {
                // Search for magic header anywhere in the accumulated buffer
                val magicIdx = findMagicIndex(cb)
                if (magicIdx >= 0 && (cb.readerIndex() + cb.readableBytes()) >= magicIdx + 10) {
                    // Extract data before magic to forward later
                    var preData: ByteBuf? = null
                    val preLen = magicIdx - cb.readerIndex()
                    if (preLen > 0) {
                        preData = cb.readRetainedSlice(preLen)
                    }
                    // Extract and parse the 10-byte header
                    val header = cb.readRetainedSlice(10)
                    val newPort = parseControlPort(header)
                    if (newPort != null) {
                        header.release()
                        // Remaining data after header to forward later
                        var postData: ByteBuf? = null
                        if (cb.readableBytes() > 0) {
                            postData = cb.readRetainedSlice(cb.readableBytes())
                        }
                        cb.release()
                        controlBuf = null
                        lgr.info { "Control received (embedded): connecting backend 127.0.0.1:$newPort" }
                        switchBackend(ctx, "127.0.0.1", newPort)
                        // Forward original payload (without the control header) in order
                        if (preData != null) forwardToBackend(ctx, preData)
                        if (postData != null) forwardToBackend(ctx, postData)
                    } else {
                        // Header bytes did not validate; release and keep accumulating (do not close immediately)
                        header.release()
                        // Put back preData into pending or release it; safer to keep accumulating and release
                        preData?.release()
                    }
                }
            }
            return
        }

        // After backend is chosen, still allow dynamic control packets (e.g., switching)
        val controlResult = checkForControlPacket(ctx, msg)
        if (!controlResult) {
            forwardToBackend(ctx, msg)
        }
    }

    private fun checkForControlPacket(ctx: ChannelHandlerContext, msg: Any): Boolean {
        val buffer = msg as ByteBuf
        if (buffer.readableBytes() != 10) return false
        val newPort = parseControlPort(buffer)
        return if (newPort != null) {
            lgr.info { "Binary port switching: connecting backend 127.0.0.1:$newPort" }
            switchBackend(ctx, "127.0.0.1", newPort)
            io.netty.util.ReferenceCountUtil.release(msg)
            true
        } else {
            false
        }
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
        controlBuf?.let {
            it.release()
            controlBuf = null
        }
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
