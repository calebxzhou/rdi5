package calebxzhou.rdi.ihq.net

import calebxzhou.rdi.ihq.GAME_PORT
import io.ktor.network.selector.SelectorManager
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ServerChannel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.flow.FlowControlHandler
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.util.concurrent.DefaultThreadFactory

object GameNetServer {
    val channel: Class<out ServerChannel> = if (Epoll.isAvailable())
        EpollServerSocketChannel::class.java
    else
        NioServerSocketChannel::class.java
    val eventGroup = if (Epoll.isAvailable())
        EpollEventLoopGroup(0, DefaultThreadFactory("RDI-Epoll"))
    else
        NioEventLoopGroup(0, DefaultThreadFactory("RDI-Nio"))

    fun start(selectorManager: SelectorManager) {
        ServerBootstrap()
            .channel(channel)
            .group(eventGroup)
            .option(ChannelOption.TCP_NODELAY, true)
            .localAddress("::", GAME_PORT)
            .handler(object : ChannelInitializer<ServerChannel>() {
                override fun initChannel(channel: ServerChannel) {
                    channel.pipeline()
                        .addLast("timeout", ReadTimeoutHandler(15))
                        .addLast("splitter", RFrameDecoder())
                        .addLast(FlowControlHandler())
                        .addLast("decoder", RPacketDecoder())
                        .addLast("packet_handler", RPacketReceiver())
                        .addLast("encoder", RPacketEncoder())
                        .addLast("prepender", RFrameEncoder())

                }

            })
    }
}