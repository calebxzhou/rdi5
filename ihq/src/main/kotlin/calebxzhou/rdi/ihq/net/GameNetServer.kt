package calebxzhou.rdi.ihq.net

import calebxzhou.rdi.ihq.GAME_PORT
import calebxzhou.rdi.ihq.net.protocol.CAbortPacket
import io.ktor.network.selector.*
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.flow.FlowControlHandler
import io.netty.util.concurrent.DefaultThreadFactory

object GameNetServer {
    lateinit var chafu: ChannelFuture
    val channel: Class<out ServerChannel> = if (Epoll.isAvailable())
        EpollServerSocketChannel::class.java
    else
        NioServerSocketChannel::class.java
    val eventGroup = if (Epoll.isAvailable())
        EpollEventLoopGroup(0, DefaultThreadFactory("RDI-Epoll"))
    else
        NioEventLoopGroup(0, DefaultThreadFactory("RDI-Nio"))

    fun start(selectorManager: SelectorManager)  {
        RPacketSet
        chafu= ServerBootstrap()
            .channel(channel)
            .group(eventGroup)
            .localAddress("0.0.0.0", GAME_PORT)
            .childHandler(object : ChannelInitializer<Channel>() {
                override fun initChannel(channel: Channel) {
                    channel.config().setOption<Boolean?>(ChannelOption.TCP_NODELAY, true)

                    channel.pipeline()
                       // .addLast("timeout", ReadTimeoutHandler(15))
                        .addLast("splitter", RFrameDecoder())
                        .addLast(FlowControlHandler())
                        .addLast("decoder", RPacketDecoder())
                        .addLast("prepender", RFrameEncoder())
                        .addLast("encoder", RPacketEncoder())
                        .addLast("packet_handler", RPacketReceiver())

                }

            }).bind()
            .syncUninterruptibly();
    }
    fun ChannelHandlerContext.sendPacket(packet: CPacket) {
        this.writeAndFlush(packet)
    }
    fun ChannelHandlerContext.abort(reason: String="") {
        sendPacket(CAbortPacket(reason))
        close()
    }
}