package calebxzhou.rdi.net

import calebxzhou.rdi.Const
import calebxzhou.rdi.exception.RequestError
import calebxzhou.rdi.lgr
import calebxzhou.rdi.model.Response
import calebxzhou.rdi.ui2.component.alertErr
import calebxzhou.rdi.ui2.component.closeLoading
import calebxzhou.rdi.ui2.component.showLoading
import calebxzhou.rdi.ui2.frag.LoginFragment
import calebxzhou.rdi.ui2.frag.UpdateFragment
import calebxzhou.rdi.ui2.goto
import calebxzhou.rdi.ui2.nowFragment
import calebxzhou.rdi.util.error
import calebxzhou.rdi.util.ioScope
import calebxzhou.rdi.util.ioTask
import calebxzhou.rdi.util.isMcStarted
import calebxzhou.rdi.util.serdesJson
import io.ktor.client.call.*
import io.ktor.client.plugins.compression.compress
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.sse.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import net.minecraft.client.multiplayer.ServerData

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

           if (!noUpdate && isMcStarted) {
               goto(UpdateFragment(this))
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
        return httpRequest {
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
            lgr.info("[HQ REQ] ${method.value} /$path $paramsStr")
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

    inline fun requestU(
        path: String,
        method: HttpMethod = HttpMethod.Post,
        params: Map<String, Any> = mapOf(),
        showLoading: Boolean = true,
        body: String?=null,
        crossinline onErr: (Response<Unit>) -> Unit = { alertErr(it.msg) },
        crossinline onOk: (Response<Unit>) -> Unit,
    ) = request<Unit>(path, method, params, showLoading, body,onErr, onOk)

    inline fun <reified T> request(
        path: String,
        method: HttpMethod = HttpMethod.Get,
        params: Map<String, Any> = mapOf(),
        showLoading: Boolean = true,
        body: String?=null,
        crossinline onErr: (Response<T>) -> Unit = { alertErr(it.msg) },
        crossinline onOk: suspend (Response<T>) -> Unit,
    ) {
        if (showLoading) {
            nowFragment?.showLoading()
        }
        ioScope.launch {
            try {
                val req = makeRequest<T>(path, method, params){
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



    fun sse(
        path: String,
        params: Map<String, Any?> = emptyMap(),
        bufferPolicy: SSEBufferPolicy? = null,
        configureRequest: HttpRequestBuilder.() -> Unit = {},
        onError: (Throwable) -> Unit = { throwable ->
            lgr.error(throwable)
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