package calebxzhou.rdi.service

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets

/**
 * calebxzhou @ 2025-10-28 10:29
 */
val client = HttpClient(OkHttp){
    install(WebSockets)
}