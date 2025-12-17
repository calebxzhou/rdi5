package calebxzhou.rdi.connect.server

import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

fun main() {
    embeddedServer(Netty, port = 65240, module = Application::connectModule).start(wait = true)
}

fun Application.connectModule() {
    install(WebSockets)

    routing {
        get("/health") {
            call.respondText("OK")
        }

        webSocket("/ws") { handleSocket() }
    }
}

private suspend fun DefaultWebSocketServerSession.handleSocket() {
    send(Frame.Text(json.encodeToString(HelloMessage())))
    for (frame in incoming) {
        when (frame) {
            is Frame.Text -> send(Frame.Text(frame.readText()))
            is Frame.Close -> {
                close(CloseReason(frame.readReason()?.code ?: 1000, "client closed"))
                return
            }
            else -> continue
        }
    }
}

@Serializable
private data class HelloMessage(val type: String = "hello")
