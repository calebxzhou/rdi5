package calebxzhou.rdi.net

import calebxzhou.rdi.auth.RAccount
import calebxzhou.rdi.ui.screen.RLoginScreen
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.mc
import io.ktor.client.statement.HttpResponse
import io.ktor.util.encodeBase64
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.util.concurrent.DefaultThreadFactory

class RServer(
    val ip: String,
    val gamePort: Int,
    val hqPort: Int
) {
    lateinit var chafu: ChannelFuture

    companion object {
        var now: RServer? = null
        val OFFICIAL_DEBUG = RServer("127.0.0.1"
        , 28506, 28507)
        val OFFICIAL_NNG = RServer("rdi.calebxzhou.cn"
            , 28506, 28507,
        )

    }

    fun connect() {
        val channel =
            if (Epoll.isAvailable()) EpollServerSocketChannel::class.java else NioServerSocketChannel::class.java
        val eventGroup = if (Epoll.isAvailable())
            EpollEventLoopGroup(0, DefaultThreadFactory("RDI-Epoll"))
        else
            NioEventLoopGroup(0, DefaultThreadFactory("RDI-Nio"))
        chafu = Bootstrap()
            .channel(channel)
            .group(eventGroup)
            .handler(object : ChannelInitializer<Channel>() {
                override fun initChannel(channel: Channel) {
                    channel.config().setOption(ChannelOption.TCP_NODELAY, true)
                    channel.pipeline()
                        .addLast("timeout", ReadTimeoutHandler(15))
                    //.....
                }

            })
            .connect(ip, gamePort)
        mc go  RLoginScreen(this)
    }

    fun sendGamePacket(pk: SPacket) {
        chafu.channel().writeAndFlush(pk)
    }

    suspend fun prepareRequest(
        post: Boolean = false,
        path: String,
        params: List<Pair<String, Any>> = listOf(),
    ): HttpResponse {
        val fullUrl = "http://${ip}:${hqPort}/${path}"
        val headers = RAccount.now?.let {
            listOf("Authorization" to "Basic ${"${it._id}:${it.pwd}".encodeBase64()}")
        } ?: listOf()
        return httpRequest(post, fullUrl, params, headers)

    }

}