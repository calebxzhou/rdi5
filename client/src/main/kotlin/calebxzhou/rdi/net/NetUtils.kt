package calebxzhou.rdi.net

import calebxzhou.rdi.Const
import calebxzhou.rdi.lgr
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.request
import io.ktor.http.HttpMethod
import net.minecraft.network.FriendlyByteBuf

import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.nio.charset.StandardCharsets

typealias RByteBuf = FriendlyByteBuf


/**
 * calebxzhou @ 2025-04-20 15:46
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