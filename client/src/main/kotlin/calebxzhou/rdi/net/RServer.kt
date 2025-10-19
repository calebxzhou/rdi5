package calebxzhou.rdi.net

import calebxzhou.rdi.Const
import calebxzhou.rdi.lgr
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.model.Response
import calebxzhou.rdi.ui2.component.alertErr
import calebxzhou.rdi.ui2.component.closeLoading
import calebxzhou.rdi.ui2.component.showLoading
import calebxzhou.rdi.ui2.frag.LoginFragment
import calebxzhou.rdi.ui2.goto
import calebxzhou.rdi.ui2.nowFragment
import calebxzhou.rdi.util.encodeBase64
import calebxzhou.rdi.util.ioScope
import calebxzhou.rdi.util.serdesJson
import io.ktor.client.call.body
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.minecraft.client.multiplayer.ServerData
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

val server
    get() = RServer.now

class RServer(
    val ip: String,
    val bgpIp: String,
    val hqPort: Int,
    var gamePort: Int,
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

    companion object {
        val OFFICIAL_DEBUG = RServer(
            "127.0.0.1", "127.0.0.1", 65231, 65230
        )
        val OFFICIAL_NNG = RServer(
            "rdi.calebxzhou.cn", "b5rdi.calebxzhou.cn", 65231, 65230
        )
        var now: RServer = if (Const.DEBUG) OFFICIAL_DEBUG else OFFICIAL_NNG
    }


    fun connect() {

        /*   if (!noUpdate && isMcStarted) {
               goto(UpdateFragment(this))
           } else {*/
        goto(LoginFragment())
        //  }
    }

    suspend inline fun createRequest(
        path: String,
        method: HttpMethod,
        params: Map<String, Any> = mapOf(),
        crossinline builder: HttpRequestBuilder.() -> Unit={}
    ): HttpResponse {
        return httpRequest {
            url("$hqUrl/${path}")
            this.method = method
            RAccount.now?.let {
                header("Authorization", "Basic ${"${it._id}:${it.pwd}".encodeBase64}")
            }
            builder()
        }
    }

    suspend inline fun <reified T> request(
        path: String,
        method: HttpMethod = HttpMethod.Get,
        params: Map<String, Any> = mapOf(),
        crossinline builder: HttpRequestBuilder.() -> Unit={}
    ): Response<T> {
        return createRequest(path, method, params,builder).body<Response<T>>()
    }
    inline fun requestU(
        path: String,
        method: HttpMethod = HttpMethod.Get,
        params: Map<String, Any> = mapOf(),
        showLoading: Boolean,
        crossinline onErr: (Response<Unit>) -> Unit = { alertErr(it.msg) },
        crossinline onOk: (Response<Unit>) -> Unit,
    ) = request<Unit>(path,method,params,showLoading,onErr,onOk)

    inline fun <reified T> request(
        path: String,
        method: HttpMethod = HttpMethod.Get,
        params: Map<String, Any> = mapOf(),
        showLoading: Boolean,
        crossinline onErr: (Response<T>) -> Unit = { alertErr(it.msg) },
        crossinline onOk: (Response<T>) -> Unit,
    ) {
        if (showLoading) {
            nowFragment?.showLoading()
        }
        ioScope.launch {
            try {
                val req = request<T>(path, method, params)
                if (showLoading)
                    nowFragment?.closeLoading()
                if (req.ok) {
                    onOk(req)
                } else {
                    onErr(req)
                    lgr.error("req error ${req.msg}")
                }
            } catch (e: Exception) {
                if (showLoading)
                    nowFragment?.closeLoading()
                alertErr("请求失败: ${e.message}")
                e.printStackTrace()
            }


        }
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
            httpStringRequest_(post, fullUrl, params, headers)
        val body = resp.body
        if (Const.DEBUG) lgr.info(resp.statusCode().toString() + " " + body)
        return serdesJson.decodeFromString<Response<T>>(body)


    }
    @Deprecated("use request instead")
    fun hqRequest(
        post: Boolean = false,
        path: String,
        showLoading: Boolean = true,
        params: List<Pair<String, Any>> = listOf(),
        onErr: (Response<*>) -> Unit = { alertErr(it.msg) },
        onOk: (Response<*>) -> Unit
    ) = hqRequestT<Unit>(post, path, showLoading, params, onErr = onErr, onOk = onOk)
    @Deprecated("use request instead")
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
                if (showLoading)
                    nowFragment?.closeLoading()
                if (req.ok) {
                    onOk(req)
                } else {
                    onErr(req)
                    lgr.error("req error ${req.msg}")
                }
            } catch (e: Exception) {
                if (showLoading)
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
        val cancelled = AtomicBoolean(false)
        val job: Job = ioScope.launch(Dispatchers.IO) {
            var response: io.ktor.client.statement.HttpResponse? = null
            try {
                response = ktorHttpClient.get(url) {
                    header(HttpHeaders.Accept, "text/event-stream")
                    header(HttpHeaders.CacheControl, "no-cache")
                    header(HttpHeaders.UserAgent, WEB_USER_AGENT)
                    if (authHeader != null) {
                        header(HttpHeaders.Authorization, authHeader)
                    }
                    timeout {
                        requestTimeoutMillis = 0
                        socketTimeoutMillis = 0
                    }
                }
                if (response.status.value !in 200..299) {
                    onError(IllegalStateException("SSE HTTP ${response.status.value}")); return@launch
                }
                val channel = response.bodyAsChannel()
                channel.toInputStream().bufferedReader(StandardCharsets.UTF_8).use { reader ->
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
                        if (line.isEmpty()) {
                            dispatch()
                            continue
                        }
                        when {
                            line.startsWith("event:") -> event = line.substring(6).trim()
                            line.startsWith(":") -> Unit
                            line.startsWith("data:") -> dataBuf.append(line.substring(5).trim()).append('\n')
                        }
                    }
                    dispatch()
                }
            } catch (e: Exception) {
                if (!cancelled.get()) onError(e)
            } finally {
                onClosed()
            }
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
        if (ev == "done") {
            return@openLogSse
        }
        if (data == "💓" || ev == "hello") return@openLogSse
        // Multi-line payload possible
        data.lines().filter { it.isNotBlank() }.forEach { onLine(it) }
    }, onError = onError, onClosed = onClosed)
}