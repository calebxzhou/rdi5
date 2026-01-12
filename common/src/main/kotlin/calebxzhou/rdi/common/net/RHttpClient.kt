package calebxzhou.rdi.common.net

import calebxzhou.rdi.common.DIR
import calebxzhou.rdi.common.serdesJson
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.plugins.cache.storage.*
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

suspend inline fun httpRequest(crossinline builder: HttpRequestBuilder.() -> Unit): HttpResponse = ktorClient.request(builder)
fun HttpRequestBuilder.json() = contentType(ContentType.Application.Json)
private val CACHE_DIR = DIR.resolve("cache").resolve("http").apply { mkdirs() }
private const val HTTP_CACHE_SIZE_BYTES = 4*1024L * 1024 * 1024 // 1GB

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
                    cache(Cache(CACHE_DIR, HTTP_CACHE_SIZE_BYTES))
                }
            }
            BrowserUserAgent()
            install(ContentNegotiation) {
                json(serdesJson)

            }
            install(HttpCache) {
                publicStorage(FileStorage(CACHE_DIR))
            }
            install(SSE) {
                maxReconnectionAttempts = 4
                reconnectionTime = 5.seconds
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
