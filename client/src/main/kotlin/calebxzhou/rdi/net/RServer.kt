package calebxzhou.rdi.net

import calebxzhou.rdi.Const
import calebxzhou.rdi.lgr
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.ui2.frag.LoadingFragment
import calebxzhou.rdi.ui2.frag.SelectAccountFragment
import calebxzhou.rdi.ui2.frag.UpdateFragment
import calebxzhou.rdi.ui2.component.alertErr
import calebxzhou.rdi.util.encodeBase64
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.ioScope
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.uiThread
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.flow.FlowControlHandler
import io.netty.util.concurrent.DefaultThreadFactory
import kotlinx.coroutines.launch
import net.minecraft.client.multiplayer.ServerData
import java.net.http.HttpResponse

class RServer(
    val ip: String,
    val bgpIp: String,
    val hqPort: Int,
    var gamePort : Int,
) {
    val noUpdate = System.getProperty("rdi.noUpdate").toBoolean()
    val mcData
        get() = { bgp: Boolean ->
            ServerData(
                "RDI",
                "${if (bgp) bgpIp else ip}:${gamePort}",
                ServerData.Type.OTHER
            )
        }
    val hqUrl = "http://${ip}:${hqPort}/"
    val channel = if(Epoll.isAvailable()) EpollSocketChannel::class.java else NioSocketChannel::class.java
    val eventGroup = if(Epoll.isAvailable())
        EpollEventLoopGroup(0, DefaultThreadFactory("RDI-Epoll"))
    else
        NioEventLoopGroup(0, DefaultThreadFactory("RDI-Nio"))
    var channelFuture: ChannelFuture? = null
    companion object {
        var now: RServer? = null
        val OFFICIAL_DEBUG = RServer(
            "127.0.0.1", "127.0.0.1",65231,65230
        )

        val default: RServer
            get() = now ?: OFFICIAL_DEBUG
    }

    fun connectGhq(){
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
            .connect(ip,gamePort)
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
    }
    fun connect() {

        if (!noUpdate) {
            mc go UpdateFragment(this)
        } else {
            mc go SelectAccountFragment(this)
        }
    }
    fun disconnectGhq() {
        channelFuture?.channel()?.close()
        // eventGroup.shutdownGracefully()
        channelFuture = null
    }
    fun sendGhq(packet: SPacket){
        channelFuture?.channel()?.writeAndFlush(packet)
    }


    suspend fun prepareRequest(
        post: Boolean = false,
        path: String,
        params: List<Pair<String, Any>> = listOf(),
    ): HttpResponse<String> {
        val fullUrl = "http://${ip}:${hqPort}/${path}"
        val headers = RAccount.now?.let {
            listOf("Authorization" to "Basic ${"${it._id}:${it.pwd}".encodeBase64}")
        } ?: listOf()
        return httpStringRequest(post, fullUrl, params, headers)

    }

    fun hqRequest(
        post: Boolean = false,
        path: String,
        showLoading: Boolean = true,
        params: List<Pair<String, Any>> = listOf(),
        onOk: (HttpResponse<String>) -> Unit
    ) {
        var frag: LoadingFragment? = null
        if (showLoading) {
            uiThread {
                frag = LoadingFragment()
                mc go frag
            }
        }
        ioScope.launch {
            val req = prepareRequest(post, path, params)
            LoadingFragment.close()
            if (req.success) {
                if(Const.DEBUG) lgr.info(req.body)
                onOk(req)
            }else{
                alertErr("请求错误: ${req.statusCode()} ${req.body}")
                lgr.error("${req.statusCode()} ${req.body}")
            }


        }
    }
}