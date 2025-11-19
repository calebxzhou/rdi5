package calebxzhou.rdi.util

import calebxzhou.rdi.model.Response
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * calebxzhou @ 2025-09-29 15:04
 */
suspend fun RoutingContext.gson(obj: Any) {
    call.respondText(
        Response(0,"", obj).gson,
        ContentType.Application.Json,
        HttpStatusCode.OK
    )
}
suspend fun RoutingContext.json(obj: Any) {
    response(data=obj)
}
suspend fun <T> RoutingContext.response(ok:Boolean=true, msg: String="", data: T? = null) {
    call.respondText(
        serdesJson.encodeToString(Response(if (ok) 0 else -1, msg, data)),
        ContentType.Application.Json,
        HttpStatusCode.OK
    )
}