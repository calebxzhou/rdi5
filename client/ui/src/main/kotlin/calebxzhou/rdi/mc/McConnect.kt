package calebxzhou.rdi.mc

import calebxzhou.rdi.client.common.protocol.PingCommand
import calebxzhou.rdi.client.common.protocol.WsMessage
import calebxzhou.rdi.client.common.protocol.WsRequest
import calebxzhou.rdi.client.common.protocol.WsResponse
import calebxzhou.rdi.common.util.ioTask
import calebxzhou.rdi.lgr
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * calebxzhou @ 2025-12-15 23:38
 */
private val requestCounter = AtomicInteger(1)
private val mcJson = Json {
    ignoreUnknownKeys = true
    classDiscriminator = "type"
}

val pending = ConcurrentHashMap<Int, CompletableDeferred<WsResponse<*>>>()
var mcSession: DefaultWebSocketServerSession? = null
private val sendMutex = Mutex()
fun startLocalServer(wait: Boolean = false) {
    embeddedServer(Netty, host = "127.0.0.1", port = 65240) {
        install(WebSockets)
        routing {
            webSocket("/") {
                mcSession = this
                val receiverJob = launch {
                    for (frame in incoming) {
                        when (frame) {
                            is Frame.Text -> handleClientFrame(frame.readText())
                            is Frame.Close -> {
                                val reason = frame.readReason()?.message ?: "no reason"
                                lgr.info { "mc client disconnect $reason" }
                                mcSession = null
                                return@launch
                            }

                            else -> lgr.warn { "Unsupported WebSocket frame: ${frame.frameType}" }
                        }
                    }
                }
                val protocolJob = launch { runProtocol() }
                try {
                    receiverJob.join()
                } finally {
                    protocolJob.cancelAndJoin()
                    receiverJob.cancelAndJoin()
                }
            }
        }
    }.start(wait = wait)
}

private suspend fun DefaultWebSocketServerSession.runProtocol() {
    val version = testConnection()
    if (version != null) {
        lgr.info { "MC reports $version" }
    } else {
        lgr.warn { "MC client no response, end " }
    }
}

private fun DefaultWebSocketServerSession.handleClientFrame(
    text: String
) {
    runCatching { mcJson.decodeFromString<WsMessage>(text) }
        .onFailure { error -> lgr.error(error) { "Failed to decode client payload: $text" } }
        .onSuccess { payload ->
            payload.handle()
        }
}

fun WsMessage.send() = ioTask {
    mcSession?.send(this)
}

suspend fun DefaultWebSocketServerSession.send(msg: WsMessage) {
    val payload = mcJson.encodeToString(msg)
    sendMutex.withLock {
        send(Frame.Text(payload))
    }
}

suspend fun <T : Any> DefaultWebSocketServerSession.request(msg: WsMessage): WsResponse<T> {
    val reqId = requestCounter.getAndIncrement()
    val deferred = CompletableDeferred<WsResponse<T>>()
    @Suppress("UNCHECKED_CAST")
    pending[reqId] = deferred as CompletableDeferred<WsResponse<*>>
    send(WsRequest(reqId, msg))
    return deferred.await().also {
        pending.remove(reqId)
    }
}

private suspend fun DefaultWebSocketServerSession.testConnection(
): Boolean {
    val resp = request<String>(PingCommand())
    return resp.msg == "ok"
}

private fun WsMessage.handle() {
    when (this) {
        is WsResponse<*> -> {
            val deferred = pending[reqId]
            if (deferred != null) {
                deferred.complete(this)
            } else {
                lgr.warn { "Unexpected ws response for req $reqId" }
            }
        }
        is WsRequest<*> -> {

        }

        else -> {
            lgr.warn { "ws-server not support frame $this" }
        }
    }
}
private fun <T : WsMessage> WsRequest<T>.handle() {

}