package calebxzhou.rdi.net

import calebxzhou.rdi.Const
import calebxzhou.rdi.lgr
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.model.Response
import calebxzhou.rdi.ui2.frag.UpdateFragment
import calebxzhou.rdi.ui2.component.alertErr
import calebxzhou.rdi.ui2.component.closeLoading
import calebxzhou.rdi.ui2.component.showLoading
import calebxzhou.rdi.ui2.frag.LoginFragment
import calebxzhou.rdi.ui2.goto
import calebxzhou.rdi.ui2.nowFragment
import calebxzhou.rdi.util.encodeBase64
import calebxzhou.rdi.util.ioScope
import calebxzhou.rdi.util.isMcStarted
import calebxzhou.rdi.util.serdesJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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
import net.minecraft.client.multiplayer.ServerData
import java.net.http.HttpResponse
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean

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

        val OFFICIAL_DEBUG = RServer(
            "127.0.0.1", "127.0.0.1",65231,65230
        )
        val OFFICIAL_NNG = RServer(
            "rdi.calebxzhou.cn", "b5rdi.calebxzhou.cn",65231,65230
        )
        var now: RServer = if(Const.DEBUG) OFFICIAL_DEBUG else OFFICIAL_NNG
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

     /*   if (!noUpdate && isMcStarted) {
            goto(UpdateFragment(this))
        } else {*/
            goto(LoginFragment())
      //  }
    }
    fun disconnectGhq() {
        channelFuture?.channel()?.close()
        // eventGroup.shutdownGracefully()
        channelFuture = null
    }
    fun sendGhq(packet: SPacket){
        channelFuture?.channel()?.writeAndFlush(packet)
    }


    suspend inline fun <reified T> prepareRequest(
        post: Boolean = false,
        path: String,
        params: List<Pair<String, Any>> = listOf(),
    ): Response<T> {
        val fullUrl = "http://${ip}:${hqPort}/${path}"
        val headers = RAccount.now?.let {
            listOf("Authorization" to "Basic ${"${it._id}:${it.pwd}".encodeBase64}")
        } ?: listOf()
        val resp =
            httpStringRequest(post, fullUrl, params, headers)
        val body = resp.body
        if(Const.DEBUG) lgr.info(resp.statusCode().toString()+" "+ body)
        return serdesJson.decodeFromString<Response<T>>(body)


    }

    fun hqRequest(
        post: Boolean = false,
        path: String,
        showLoading: Boolean = true,
        params: List<Pair<String, Any>> = listOf(),
        onErr: (Response<*>) -> Unit = { alertErr(it.msg) },
        onOk: (Response<*>) -> Unit
    ) = hqRequestT<Unit>(post, path, showLoading, params, onErr = onErr, onOk = onOk)

    inline fun <reified T> hqRequestT(
        post: Boolean = false,
        path: String,
        showLoading: Boolean = true,
        params: List<Pair<String, Any>> = listOf(),
        crossinline onErr: (Response<T>) -> Unit = { alertErr(it.msg) },
        crossinline onOk: (Response<T>) -> Unit,
    ) {
        if (showLoading) {
            nowFragment?.showLoading()
        }
        ioScope.launch {
            try {
                val req = prepareRequest<T>(post, path, params)
                if(showLoading)
                    nowFragment?.closeLoading()
                if (req.ok) {
                    onOk(req)
                }else{
                    onErr(req)
                    lgr.error("req error ${req.msg}")
                }
            } catch (e: Exception) {
                if(showLoading)
                    nowFragment?.closeLoading()
                alertErr("请求失败: ${e.message}")
                e.printStackTrace()
            }


        }
    }

    /**
     * Open a Server-Sent Events stream to /room/log/stream.
     * Returns a close function to terminate the stream.
     */
    fun openLogSse(
        onEvent: (event: String?, data: String) -> Unit,
        onError: (Throwable) -> Unit = {},
        onClosed: () -> Unit = {}
    ): () -> Unit {
        val authHeader = RAccount.now?.let { "Basic ${"${it._id}:${it.pwd}".encodeBase64}" }
        val url = "http://${ip}:${hqPort}/room/log/stream"
        val client = HttpClient.newBuilder().build()
        val reqBuilder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .GET()
        if (authHeader != null) reqBuilder.header("Authorization", authHeader)
        val request = reqBuilder.build()
        val cancelled = AtomicBoolean(false)
        val job: Job = ioScope.launch(Dispatchers.IO) {
            try {
                val resp = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
                if (resp.statusCode() !in 200..299) {
                    onError(IllegalStateException("SSE HTTP ${resp.statusCode()}")); return@launch
                }
                BufferedReader(InputStreamReader(resp.body(), StandardCharsets.UTF_8)).use { reader ->
                    var event: String? = null
                    val dataBuf = StringBuilder()
                    fun dispatch() {
                        if (dataBuf.isNotEmpty()) {
                            onEvent(event, dataBuf.toString().trimEnd())
                            dataBuf.setLength(0)
                            event = null
                        }
                    }
                    while (!cancelled.get()) {
                        val line = reader.readLine() ?: break
                        if (line.isEmpty()) { dispatch(); continue }
                        when {
                            line.startsWith("event:") -> event = line.substring(6).trim()
                            line.startsWith(":") -> { /* comment / heartbeat */ }
                            line.startsWith("data:") -> dataBuf.append(line.substring(5).trim()).append('\n')
                        }
                    }
                    dispatch()
                }
            } catch (e: Exception) {
                if (!cancelled.get()) onError(e)
            } finally { onClosed() }
        }
        return {
            cancelled.set(true)
            job.cancel()
        }
    }

    /** Convenience wrapper to consume only log lines (ignores control events). */
    fun openLogLineStream(
        onLine: (String) -> Unit,
        onError: (Throwable) -> Unit = {},
        onClosed: () -> Unit = {}
    ): () -> Unit = openLogSse(onEvent = { ev, data ->
        if (ev == "error") {
            onError(IllegalStateException(data)); return@openLogSse
        }
        if (ev == "done") { return@openLogSse }
        if (data == "💓" || ev == "hello") return@openLogSse
        // Multi-line payload possible
        data.lines().filter { it.isNotBlank() }.forEach { onLine(it) }
    }, onError = onError, onClosed = onClosed)
}