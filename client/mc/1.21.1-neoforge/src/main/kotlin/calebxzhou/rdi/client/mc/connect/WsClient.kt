package calebxzhou.rdi.client.mc.connect

import calebxzhou.mykotutils.log.Loggers
import calebxzhou.rdi.client.mc.*
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.launch
typealias WsSession = DefaultClientWebSocketSession
var wsSession: WsSession? = null
object WsClient {
    private val lgr by Loggers
    val wsClient = HttpClient(OkHttp){
        install(WebSockets){
            contentConverter = KotlinxWebsocketSerializationConverter(serdesJson)
        }

    }
    suspend fun connectCore(){
        wsClient.webSocket("ws://127.0.0.1:65240/") {
            lgr.info { "rdi core connected" }
            wsSession = this
            handleMsg()
        }
    }
    private suspend fun WsSession.handleMsg() {
        while (true) {
            val msg = try {
                receiveDeserialized<WsMessage>()
            } catch (closed: ClosedReceiveChannelException) {
                lgr.info { "WebSocket channel closed, stop receiving commands" }
                return
            } catch (throwable: Throwable) {
                lgr.error(throwable) { "Failed to parse command" }
                return
            }
            lgr.info { "incoming msg: ${msg.json}" }
            msg.handle(this)
        }
    }
    fun WsMessage.send() = scope.launch {
        wsSession?.sendMessage(this@send)?: lgr.warn("session null, rdi gui not connect")
    }
    suspend fun <T> WsSession.response(reqId: Int,msg: T){ sendMessage(Response(reqId,msg)) }
    suspend fun WsSession.sendMessage(msg: WsMessage) {
        val msg = serdesJson.encodeToString<WsMessage>(msg)
        lgr.info { "msg send: $msg" }
        send(Frame.Text(msg))
    }
}