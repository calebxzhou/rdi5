package calebxzhou.rdi.net

import calebxzhou.rdi.Const
import calebxzhou.rdi.exception.AuthError
import calebxzhou.rdi.exception.ParamError
import calebxzhou.rdi.lgr
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import java.nio.charset.StandardCharsets

/**
 * calebxzhou @ 2025-05-10 23:20
 */
val HttpResponse.success
    get() = this.status.isSuccess()
val HttpResponse.body
    get() = runBlocking { body<String>() }
suspend fun httpRequest(
    post: Boolean = false,
    url: String,
    params: List<Pair<String, Any?>> = emptyList(),
    headers: List<Pair<String, String>> = emptyList()
): HttpResponse {
    // Filter out parameters with null values
    val filteredParams = params.filter { it.second != null }

    // Debug logging (assuming similar logging setup)
    if (Const.DEBUG) {
        lgr.info("http ${if (post) "post" else "get"} $url ${filteredParams.joinToString(",")}")
    }

    // Create Ktor HTTP client
    val client = HttpClient(CIO) {
        install(io.ktor.client.plugins.compression.ContentEncoding) {
            gzip()
            deflate()
        }
    }

    return try {
        client.request(url) {
            // Set method
            method = if (post) HttpMethod.Post else HttpMethod.Get

            // Add headers
            headers.forEach { (key, value) ->
                header(key, value)
            }

            // Add parameters
            if (post) {
                // For POST, set form parameters
                setBody(
                    FormDataContent(
                        Parameters.build {
                            filteredParams.forEach { (key, value) ->
                                append(key, value.toString())
                            }
                        }
                    )
                )
                contentType(ContentType.Application.FormUrlEncoded.withCharset(StandardCharsets.UTF_8))
            } else {
                // For GET, add URL parameters
                filteredParams.forEach { (key, value) ->
                    parameter(key, value.toString())
                }
            }
        }
    } finally {
        // Ensure client is closed
        client.close()
    }
}


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

suspend fun ApplicationCall.ok(msg: String? = null,contentType: ContentType? =null) {
    msg?.run {
        respondText(this, status = HttpStatusCode.OK, contentType = contentType)
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