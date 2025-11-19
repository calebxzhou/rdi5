package calebxzhou.rdi.net

import calebxzhou.rdi.RDI
import calebxzhou.rdi.util.serdesJson
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import okhttp3.Cache
import java.net.ProxySelector
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

suspend inline  fun httpRequest(crossinline builder: HttpRequestBuilder.() -> Unit): HttpResponse = ktorClient.request(builder)
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
                bufferPolicy = SSEBufferPolicy.LastEvents(10)
            }
            install(ContentEncoding) {
                deflate(1.0F)
                gzip(0.9F)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 60_000
            }
        }
