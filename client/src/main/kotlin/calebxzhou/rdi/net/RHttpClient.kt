package calebxzhou.rdi.net

import calebxzhou.rdi.RDI
import calebxzhou.rdi.util.serdesJson
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.netty.handler.codec.compression.StandardCompressionOptions.deflate
import io.netty.handler.codec.compression.StandardCompressionOptions.gzip
import okhttp3.Cache
import java.net.ProxySelector
import java.util.concurrent.TimeUnit
import javax.print.attribute.standard.Compression
import kotlin.time.Duration.Companion.seconds

suspend fun httpRequest(builder: HttpRequestBuilder.() -> Unit): HttpResponse = ktorClient.request(builder)
fun HttpRequestBuilder.json() = contentType(ContentType.Application.Json)
private val httpCacheDirectory by lazy {
    java.io.File(java.io.File(RDI.DIR, "cache"), "http").apply { mkdirs() }
}

private const val HTTP_CACHE_SIZE_BYTES = 1024L * 1024 * 1024 // 1GB

val ktorClient
    get() =
        HttpClient(OkHttp) {
            expectSuccess = false
            engine {
                config {
                    followRedirects(true)
                    connectTimeout(10, TimeUnit.SECONDS)
                    readTimeout(0, TimeUnit.SECONDS)
                    ProxySelector.getDefault()?.let { proxySelector(it) }
                    cache(Cache(httpCacheDirectory, HTTP_CACHE_SIZE_BYTES))
                }
            }
            BrowserUserAgent()
            install(ContentNegotiation) {
                json(serdesJson)

            }
            install(SSE ){
                maxReconnectionAttempts = 4
                reconnectionTime = 2.seconds
            }
            install(ContentEncoding) {
                deflate(1.0F)
                gzip(0.9F)
            }
        }
