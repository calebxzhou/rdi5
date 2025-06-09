package calebxzhou.rdi.net

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.epoll.EpollSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.flow.FlowControlHandler
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.util.concurrent.DefaultThreadFactory

object GameNetClient {
    val channel = if(Epoll.isAvailable()) EpollSocketChannel::class.java else NioSocketChannel::class.java
    val eventGroup = if(Epoll.isAvailable())
        EpollEventLoopGroup(0, DefaultThreadFactory("RDI-Epoll"))
    else
        NioEventLoopGroup(0, DefaultThreadFactory("RDI-Nio"))

    private val connection = Bootstrap()
        .group(eventGroup)
        .channel(channel)
        .option(ChannelOption.TCP_NODELAY, true)
        .handler(object : ChannelInitializer<Channel>() {
            override fun initChannel(ch: Channel) {
                ch.pipeline().apply {

                    addLast("timeout", ReadTimeoutHandler(15))
                    addLast("splitter", RFrameDecoder())
                    addLast(FlowControlHandler())
                    addLast("decoder", RPacketDecoder())
                    addLast("packet_handler", RPacketReceiver())
                    addLast("prepender", RFrameEncoder())
                    addLast("encoder", RPacketEncoder())

                }
            }
        })
        .bind(0)
        .syncUninterruptibly()
    fun send(packet: SPacket){
        connection.channel().writeAndFlush(packet)
    }
}