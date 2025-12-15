package calebxzhou.rdi.client.mc

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import net.neoforged.fml.common.Mod
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory

val lgr = LoggerFactory.getLogger("rdi")
val logMarker
    get() = { marker: String -> MarkerFactory.getMarker(marker) }
val json = Json {
    ignoreUnknownKeys = true  // Good for forward compatibility
    classDiscriminator = "type"  // Uses the @SerialName as discriminator
}
val wsClient = HttpClient(OkHttp){
    install(WebSockets){
        contentConverter = KotlinxWebsocketSerializationConverter(json)
    }

}
@Mod("rdi")
class RDI {

    companion object {
        @JvmField
        val IHQ_URL =
            System.getProperty("rdi.ihq.url") ?: throw IllegalArgumentException("启动方式错误：找不到服务器地址1")

        @JvmField
        val GAME_IP =
            System.getProperty("rdi.game.ip") ?: throw IllegalArgumentException("启动方式错误：找不到服务器地址2")

        @JvmField
        val HOST_NAME =
            System.getProperty("rdi.host.name") ?: throw IllegalArgumentException("启动方式错误：找不到主机名")

        @JvmField
        var HOST_PORT = System.getProperty("rdi.host.port")?.toInt() ?: throw IllegalArgumentException("启动方式错误：找不到服务器端口")
        private val scope = CoroutineScope(Dispatchers.IO + Job())


        private suspend fun DefaultClientWebSocketSession.handleCommands() {
            while (true) {
                val command = runCatching { receiveDeserialized<Command>() }.getOrElse { throwable ->
                    lgr.error("Failed to parse command", throwable)
                    return
                }
                when (command) {
                    is AddChatMsgCommand -> command.handle()
                    is ChangeHostCommand -> command.handle()
                    is GetVersionIdCommand -> command.handle(this)

                    else -> {}
                }
            }
        }


        suspend fun DefaultClientWebSocketSession.sendResponse(cmd: RequestCommand, data: String) {
            val reqId = cmd.reqId
            val response = CommandResponse(reqId = reqId, data = data)
            runCatching { sendSerialized(response) }
                .onSuccess { lgr.info("Sent response for request $reqId") }
                .onFailure { lgr.error("Failed to send response for request $reqId", it) }
        }

    }


    init {
        lgr.info("RDI启动中")
        scope.launch {
            runCatching {
                wsClient.webSocket("ws://$GAME_IP:$HOST_PORT/ws") {
                    lgr.info("WebSocket connected")
                    handleCommands()
                }
            }.onFailure { error ->
                lgr.error("WebSocket connection failed", error)
            }
        }
    }
}