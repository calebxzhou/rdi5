package calebxzhou.rdi.util

import ca.weblite.objc.RuntimeUtils.msg
import calebxzhou.rdi.model.Response
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext

/**
 * calebxzhou @ 2025-09-29 15:04
 */
suspend fun RoutingContext.gson(obj: Any) {
    call.respondText(
        Response(0,"", obj).json,
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