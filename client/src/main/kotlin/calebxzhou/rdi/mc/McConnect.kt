package calebxzhou.rdi.mc

import calebxzhou.rdi.lgr
import calebxzhou.rdi.util.ioTask
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
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * calebxzhou @ 2025-12-15 23:38
 */
val pending = ConcurrentHashMap<Int, CompletableDeferred<String>>()
var mcSession: DefaultWebSocketServerSession? = null
private val sendMutex = Mutex()
fun startLocalServer(wait: Boolean = false){
    embeddedServer(Netty, host = "127.0.0.1", port = 65240){
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
private val requestCounter = AtomicInteger(1)
private val mcJson = Json {
    ignoreUnknownKeys = true
    classDiscriminator = "type"
}

private fun DefaultWebSocketServerSession.handleClientFrame(
    text: String
) {
    runCatching { mcJson.decodeFromString<LocalWsMessage>(text) }
        .onFailure { error -> lgr.error(error) { "Failed to decode client payload: $text" } }
        .onSuccess { payload ->
            when (payload) {
                is Response -> pending.remove(payload.reqId)?.complete(payload.data)
                    ?: lgr.warn { "No pending request for response ${payload.reqId}" }
                else -> lgr.info { "Client sent unsupported command type ${payload}" }
            }
        }
}
fun LocalWsMessage.send()= ioTask{
    mcSession?.sendMessage(this)
}
suspend fun DefaultWebSocketServerSession.sendMessage(msg: LocalWsMessage) {
    val payload = mcJson.encodeToString(msg)
    sendMutex.withLock {
        send(Frame.Text(payload))
    }
}

private suspend fun DefaultWebSocketServerSession.testConnection(
): String? {
    val reqId = requestCounter.getAndIncrement()
    val deferred = CompletableDeferred<String>()
    pending[reqId] = deferred
    sendMessage(PingCommand().apply { this.reqId=reqId })
    return withTimeoutOrNull(5000) {
        deferred.await()
    }.also {
        pending.remove(reqId)
    }
}