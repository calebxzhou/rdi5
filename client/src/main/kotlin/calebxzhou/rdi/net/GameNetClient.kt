package calebxzhou.rdi.net

import calebxzhou.rdi.Const
import calebxzhou.rdi.auth.RAccount
import calebxzhou.rdi.lgr
import calebxzhou.rdi.net.protocol.SMeLoginPacket
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
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
import kotlin.printStackTrace

object GameNetClient {
    val channel = if(Epoll.isAvailable()) EpollSocketChannel::class.java else NioSocketChannel::class.java
    val eventGroup = if(Epoll.isAvailable())
        EpollEventLoopGroup(0, DefaultThreadFactory("RDI-Epoll"))
    else
        NioEventLoopGroup(0, DefaultThreadFactory("RDI-Nio"))
    var channelFuture: ChannelFuture? = null
    fun connect(server: RServer): ChannelFuture? {
        val chafu = Bootstrap()
            .group(eventGroup)
            .channel(channel)
            .option(ChannelOption.TCP_NODELAY, true)
            .handler(object : ChannelInitializer<Channel>() {
                override fun initChannel(ch: Channel) {

                    ch.pipeline().apply {
                        //addLast("timeout", ReadTimeoutHandler(15))
                        addLast("splitter", RFrameDecoder())
                        addLast(FlowControlHandler())
                        addLast("decoder", RPacketDecoder())
                        addLast("prepender", RFrameEncoder())   // First frame the bytes
                        addLast("encoder", RPacketEncoder())    // Then encode packet
                        addLast("packet_handler", RPacketReceiver())
                    }
                }
            })
            .connect(server.ip,server.gamePort)
        chafu.addListener { future ->
            if (future.isSuccess) {
                lgr.info ("Successfully connected  ")
                /*if (Const.DEBUG) {
                    val account = RAccount.TESTS[System.getProperty("rdi.testAccount").toInt()]
                    GameNetClient.send(SMeLoginPacket(account.qq,account.pwd))
                }*/
                // mc go RLoginScreen(this)
            } else {
                lgr.error ("Failed to connect ")
                future.cause()?.printStackTrace()
            }
        }
        chafu.sync()
        channelFuture = chafu
        return chafu

    }
    fun send(packet: SPacket){
        channelFuture?.channel()?.writeAndFlush(packet)
    }
}