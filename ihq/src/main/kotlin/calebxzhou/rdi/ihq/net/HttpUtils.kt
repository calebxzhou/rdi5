package calebxzhou.rdi.ihq.net

import calebxzhou.rdi.ihq.exception.AuthError
import calebxzhou.rdi.ihq.exception.ParamError
import calebxzhou.rdi.ihq.model.Response
import calebxzhou.rdi.ihq.util.serdesJson
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.plugins.origin
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.util.AttributeKey
import org.bson.types.ObjectId
import java.net.ServerSocket

/**
 * calebxzhou @ 2025-05-26 12:25
 */
val httpClient
    get() = HttpClient {
        install(ContentNegotiation) {
            json(serdesJson)
        }

        install(DefaultRequest) {
            // Set Accept-Charset to prefer UTF-8 but also accept GBK
            header("Accept-Charset", "utf-8")
        }
    }


suspend fun ApplicationCall.e400(msg: String? = null) {
    err(msg)
}

suspend fun ApplicationCall.e401(msg: String? = null) {
    err(msg)
}

suspend fun ApplicationCall.e404(msg: String? = null) {
    err(msg)
}

suspend fun ApplicationCall.e500(msg: String? = null) {
    err(msg)
}

suspend fun ApplicationCall.err(msg: String? = null) {
    msg?.run {
        respondText(this, status = HttpStatusCode.OK)
    } ?: run {
        respond(HttpStatusCode.OK)
    }
}

suspend fun RoutingContext.ok(msg: String="ok") {
    response<Unit>(true,msg,null)
}
suspend fun RoutingContext.err(msg: String="❌") {
    response<Unit>(false,msg,null)
}
suspend inline fun <reified T> RoutingContext.response(ok:Boolean=true, msg: String="", data: T? = null) {
    call.response(ok,msg,data)
}
suspend inline fun <reified T> ApplicationCall.response(ok:Boolean=true, msg: String="", data: T? = null) {
    response(if(ok) 0 else -1 ,msg,data)
}
suspend inline fun <reified T> ApplicationCall.response(code: Int, msg: String="", data: T? = null) {
    respondText(
        serdesJson.encodeToString(Response(code ,msg,data)),
        ContentType.Application.Json,
        HttpStatusCode.OK
    )
}
infix fun Parameters.got(param: String): String {
    return this[param] ?: throw ParamError("参数不全")
}

val ApplicationCall.uid
    get() =
        ObjectId(this.principal<UserIdPrincipal>()?.name ?: throw AuthError("账密错"))
val RoutingContext.uid
    get() = call.uid

// ---------- Unified parameter access DSL ----------
// We sometimes need to read a parameter that could appear as:
// 1. Route/path parameter (call.parameters)
// 2. Query parameter (call.request.queryParameters)
// 3. Form / body parameter (call.receiveParameters())
// These helpers search in that order and cache form parameters so the body is only consumed once.

private val FormParamsKey = AttributeKey<Parameters>("cachedFormParams")

private suspend fun ApplicationCall.cachedFormParameters(): Parameters {
    return if (attributes.contains(FormParamsKey)) {
        attributes[FormParamsKey]
    } else {
        // receiveParameters() is safe for form-url-encoded or multipart (converted) bodies
        val p = try { receiveParameters() } catch (_: Throwable) { Parameters.Empty }
        attributes.put(FormParamsKey, p)
        p
    }
}

/**
 * Get the first present value among path, query or form parameters.
 * Throws [ParamError] if absent.
 */
suspend fun RoutingContext.param(name: String): String = call.param(name)
suspend fun RoutingContext.paramNull(name: String): String? = call.paramNull(name)
suspend fun ApplicationCall.param(name: String): String =
    paramNull(name) ?: throw ParamError("缺少参数: $name")

/** Same as [param] but returns null when absent. */
suspend fun ApplicationCall.paramNull(name: String): String? {
    // Path / route
    parameters[name]?.let { return it }
    // Query
    request.queryParameters[name]?.let { return it }
    // Form (cached)
    val form = cachedFormParameters()
    return form[name]
}

/** Try multiple alternative names, return the first found or throw. */
suspend fun ApplicationCall.param(vararg names: String): String {
    names.forEach { n -> paramNull(n)?.let { return it } }
    throw ParamError("缺少参数: ${names.joinToString("/")}")
}

/** Convenience typed accessors. Provide default if missing (no exception). */
suspend fun ApplicationCall.intParam(name: String, default: Int? = null): Int {
    val v = paramNull(name) ?: return default ?: throw ParamError("缺少参数: $name")
    return v.toIntOrNull() ?: throw ParamError("参数 $name 不是整数")
}

suspend fun ApplicationCall.longParam(name: String, default: Long? = null): Long {
    val v = paramNull(name) ?: return default ?: throw ParamError("缺少参数: $name")
    return v.toLongOrNull() ?: throw ParamError("参数 $name 不是长整数")
}

suspend fun ApplicationCall.boolParam(name: String, default: Boolean? = null): Boolean {
    val v = paramNull(name) ?: return default ?: throw ParamError("缺少参数: $name")
    return when (v.lowercase()) {
        "true", "1", "yes", "y", "on" -> true
        "false", "0", "no", "n", "off" -> false
        else -> throw ParamError("参数 $name 不是布尔值")
    }
}

suspend fun ApplicationCall.initPostParams() = receiveParameters()
suspend fun ApplicationCall.initGetParams() = request.queryParameters
val ApplicationCall.clientIp
    get() = this.request.origin.remoteAddress

val randomPort: Int
    get() = ServerSocket(0).use { it.localPort }
