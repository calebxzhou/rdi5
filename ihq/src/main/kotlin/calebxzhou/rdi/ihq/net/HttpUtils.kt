package calebxzhou.rdi.ihq.net

import calebxzhou.rdi.ihq.exception.AuthError
import calebxzhou.rdi.ihq.exception.ParamError
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import org.bson.types.ObjectId

/**
 * calebxzhou @ 2025-05-26 12:25
 */

suspend fun ApplicationCall.e400(msg: String? = null) {
    err(HttpStatusCode.BadRequest, msg)
}

suspend fun ApplicationCall.e401(msg: String? = null) {
    err(HttpStatusCode.Unauthorized, msg)
}

suspend fun ApplicationCall.e404(msg: String? = null) {
    err(HttpStatusCode.NotFound, msg)
}

suspend fun ApplicationCall.e500(msg: String? = null) {
    err(HttpStatusCode.InternalServerError, msg)
}

suspend fun ApplicationCall.err(status: HttpStatusCode, msg: String? = null) {
    msg?.run {
        respondText(this, status = status)
    } ?: run {
        respond(status)
    }
}

suspend fun ApplicationCall.ok(msg: String? = null) {
    msg?.run {
        respondText(this, status = HttpStatusCode.OK)
    } ?: run {
        respond(HttpStatusCode.OK)
    }
}

infix fun Parameters.got(param: String): String {
    return this[param] ?: throw ParamError("参数不全")
}

val ApplicationCall.uid
    get() =

        ObjectId(this.principal<UserIdPrincipal>()?.name ?: throw AuthError("无效会话"))

suspend fun ApplicationCall.initPostParams() = receiveParameters()
suspend fun ApplicationCall.initGetParams() = request.queryParameters