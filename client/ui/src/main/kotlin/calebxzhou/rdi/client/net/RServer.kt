package calebxzhou.rdi.client.net

import calebxzhou.mykotutils.ktor.DownloadProgress
import calebxzhou.mykotutils.ktor.downloadFileFrom
import calebxzhou.rdi.client.Const
import calebxzhou.rdi.client.ui.component.alertErr
import calebxzhou.rdi.client.ui.component.closeLoading
import calebxzhou.rdi.client.ui.component.showLoading
import calebxzhou.rdi.client.ui.frag.LoginFragment
import calebxzhou.rdi.client.ui.frag.UpdateFragment
import calebxzhou.rdi.client.ui.goto
import calebxzhou.rdi.client.ui.nowFragment
import calebxzhou.rdi.common.exception.RequestError
import calebxzhou.rdi.common.model.RAccount
import calebxzhou.rdi.common.model.Response
import calebxzhou.rdi.common.net.httpRequest
import calebxzhou.rdi.common.net.json
import calebxzhou.rdi.common.net.ktorClient
import calebxzhou.rdi.common.serdesJson
import calebxzhou.rdi.common.util.ioTask
import calebxzhou.rdi.lgr
import io.ktor.client.call.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.sse.*
import kotlinx.coroutines.*
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import java.net.ConnectException
import java.nio.file.Path

val server
    get() = RServer.now
var loggedAccount: RAccount = RAccount.DEFAULT

class RServer(
    val ip: String,
    val bgpIp: String,
    val hqPort: Int,
    var gamePort: Int,
) {
    val noUpdate = System.getProperty("rdi.noUpdate").toBoolean()

    /*val mcData
        get() = { bgp: Boolean ->
            ServerData(
                "RDI",
                "${if (bgp) bgpIp else ip}:${gamePort}",
                ServerData.Type.OTHER
            )
        }*/
    val hqUrl = "http://${ip}:${hqPort}"

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
        if (!noUpdate) {
            goto(UpdateFragment())
        } else {
            goto(LoginFragment())
        }
    }

    suspend inline fun createRequest(
        path: String,
        method: HttpMethod,
        params: Map<String, Any> = mapOf(),
        crossinline builder: HttpRequestBuilder.() -> Unit = {}
    ): HttpResponse {
        return try {
            httpRequest {
                url("$hqUrl/${path}")
                this.method = method
                if (method != HttpMethod.Get && params.isNotEmpty()) {
                    json()
                    setBody(
                        serdesJson.encodeToString(
                            MapSerializer(String.serializer(), JsonElement.serializer()),
                            params.mapValues {
                                JsonPrimitive(it.value.toString())
                            }
                        ))
                    compress("deflate")
                } else if (method == HttpMethod.Get && params.isNotEmpty()) {
                    params.forEach {
                        parameter(it.key, it.value)
                    }
                }
                accountAuthHeader()
                builder()
            }
        } catch (e: ConnectException) {
            e.printStackTrace()
            throw RequestError("无法连接服务器 请检查网络连接")
        }
    }

    suspend inline fun <reified T> makeRequest(
        path: String,
        method: HttpMethod = HttpMethod.Get,
        params: Map<String, Any> = mapOf(),
        crossinline builder: HttpRequestBuilder.() -> Unit = {}
    ): Response<T> {
        if (Const.DEBUG) {
            val paramsStr =
                if (params.isEmpty()) "{}" else params.entries.joinToString(", ", "{", "}") { "${it.key}=${it.value}" }
            lgr.info { "[HQ REQ] ${method.value} /$path $paramsStr" }
        }
        val response = createRequest(path, method, params, builder).body<Response<T>>()
        if (Const.DEBUG) {
            val dataStr = when (val data = response.data) {
                null -> "null"
                is String -> if (data.length > 200) data.take(200) + "..." else data
                is Collection<*> -> "[${data.size} items]"
                else -> data.toString().let { if (it.length > 200) it.take(200) + "..." else it }
            }
            lgr.info("[HQ RSP] code=${response.code} msg=${response.msg} data=$dataStr")
        }
        return response
    }

    fun HttpRequestBuilder.accountAuthHeader() {
        header(HttpHeaders.Authorization, "Bearer ${loggedAccount.jwt}")
    }

    @Deprecated("")
    inline fun _requestU(
        path: String,
        method: HttpMethod = HttpMethod.Post,
        params: Map<String, Any> = mapOf(),
        showLoading: Boolean = true,
        body: String? = null,
        crossinline onErr: (Response<Unit>) -> Unit = { alertErr(it.msg) },
        crossinline onOk: (Response<Unit>) -> Unit,
    ) = _request<Unit>(path, method, params, showLoading, body, onErr, onOk)


    @Deprecated("")
    inline fun <reified T> _request(
        path: String,
        method: HttpMethod = HttpMethod.Get,
        params: Map<String, Any> = mapOf(),
        showLoading: Boolean = true,
        body: String? = null,
        crossinline onErr: (Response<T>) -> Unit = { alertErr(it.msg) },
        crossinline onOk: suspend (Response<T>) -> Unit,
    ) {
        if (showLoading) {
            nowFragment?.showLoading()
        }
        ioTask {
            try {
                val req = makeRequest<T>(path, method, params) {
                    body?.let {
                        json()
                        setBody(it)
                    }
                }
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


    suspend inline fun download(
        path: String,
        saveTo: Path,
        noinline onProgress: (DownloadProgress) -> Unit
    ) {
        saveTo.downloadFileFrom(
            "${hqUrl}/${path}",
            mapOf(HttpHeaders.Authorization to "Bearer ${loggedAccount.jwt}"),
            onProgress = onProgress,
        )
    }

    fun sse(
        path: String,
        params: Map<String, Any?> = emptyMap(),
        bufferPolicy: SSEBufferPolicy? = null,
        configureRequest: HttpRequestBuilder.() -> Unit = {},
        onError: (Throwable) -> Unit = { throwable ->
            lgr.error { throwable }
        },
        onClosed: suspend () -> Unit = {},
        onEvent: suspend (ServerSentEvent) -> Unit,
    ) = ioTask {
        val urlString = if (path.startsWith("http", ignoreCase = true)) {
            path
        } else {
            "$hqUrl/${path.trimStart('/')}"
        }

        try {
            ktorClient.sse(urlString, {
                accountAuthHeader()
                params.forEach { (key, value) ->
                    when (value) {
                        null -> Unit
                        is Iterable<*> -> value.forEach { element -> element?.let { parameter(key, it) } }
                        is Array<*> -> value.forEach { element -> element?.let { parameter(key, it) } }
                        else -> parameter(key, value)
                    }
                }
                bufferPolicy?.let { bufferPolicy(it) }
                configureRequest()
            }) {
                try {
                    incoming.collect { event ->
                        when (event.event) {
                            "heartbeat" -> {
                                lgr.info("SSE heartbeat")
                            }

                            "error" -> onError(RequestError(event.data))
                            else -> onEvent(event)
                        }
                    }
                } finally {
                    onClosed()
                }
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (t: Throwable) {
            onError(t)
            throw t
        }
    }


}

inline fun CoroutineScope.rdiRequestU(
    path: String,
    method: HttpMethod = HttpMethod.Post,
    params: Map<String, Any> = mapOf(),
    body: String? = null,
    crossinline onDone: () -> Unit = {},
    crossinline onErr: (Throwable) -> Unit,
    crossinline onOk: (Response<Unit>) -> Unit,
) = rdiRequest<Unit>(path, method, params, body, onDone, onErr, onOk)

inline fun <reified T> CoroutineScope.rdiRequest(
    path: String,
    method: HttpMethod = HttpMethod.Get,
    params: Map<String, Any> = mapOf(),
    body: String? = null,
    crossinline onDone: () -> Unit = {},
    crossinline onErr: (Throwable) -> Unit,
    crossinline onOk: (Response<T>) -> Unit,
) = this.launch {
    runCatching {
        val req = withContext(Dispatchers.IO) {
            server.makeRequest<T>(path, method, params) {
                body?.let {
                    json()
                    setBody(it)
                }
            }
        }
        if (req.ok) {
            onOk(req)
        } else {
            lgr.error { "req error ${req.msg}" }
            throw RequestError(req.msg)
        }
    }.getOrElse {

        lgr.error(it) { "请求失败 " }
        onErr(it)
    }
    onDone()
}