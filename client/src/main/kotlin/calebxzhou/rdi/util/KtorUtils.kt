package calebxzhou.rdi.util

import io.ktor.http.ContentType
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext

/**
 * calebxzhou @ 2025-09-29 15:04
 */
suspend fun RoutingContext.json(obj: Any) {
    call.respondText(
        text = obj.json,
        contentType = ContentType.Application.Json
    )
}