package calebxzhou.rdi.connect.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.receiveDeserialized
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.use

private val json = Json { ignoreUnknownKeys = true }

fun main() = runBlocking {
    HttpClient(OkHttp) {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(json)
        }
    }.use { client ->
        client.webSocket(urlString = "ws://127.0.0.1:65240/ws") {
            sendSerialized(HelloMessage())
            val response = receiveDeserialized<HelloMessage>()
            println("Received: ${'$'}response")
        }
    }
}

@Serializable
private data class HelloMessage(val type: String = "hello")
