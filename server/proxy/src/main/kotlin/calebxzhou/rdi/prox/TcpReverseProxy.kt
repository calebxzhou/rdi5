package calebxzhou.rdi.prox

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler

/**
 * TCP Reverse Proxy using Netty with dynamic backend switching
 * Routes incoming connections to backend servers based on port in first 2 bytes
 *
 * Connection flow: Client sends 2 bytes (big-endian port), then forwards all data
 */
class TcpReverseProxy {
    //todo read mc packet  read player's uuid from login packet , get info from ihq to decide backend server
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

