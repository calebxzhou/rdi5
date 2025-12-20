package calebxzhou.rdi.master.net

import calebxzhou.rdi.master.CONF
import calebxzhou.rdi.master.ProxyConfig
import calebxzhou.rdi.master.lgr
import calebxzhou.rdi.master.util.serdesJson
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * calebxzhou @ 2025-10-16 20:42
 */

suspend inline fun httpRequest(crossinline builder: HttpRequestBuilder.() -> Unit): HttpResponse = ktorClient.request(builder)

fun HttpRequestBuilder.json() = contentType(ContentType.Application.Json)
val ktorClient
    get() =
        HttpClient(OkHttp) {
            expectSuccess = false
            engine {
                config {
                    followRedirects(true)
                    connectTimeout(10, TimeUnit.SECONDS)
                    readTimeout(0, TimeUnit.SECONDS)

                    CONF.proxy?.let { applyProxyConfig(it) }
                }
            }
            BrowserUserAgent()
            install(ContentNegotiation) {
                json(serdesJson)
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

private fun okhttp3.OkHttpClient.Builder.applyProxyConfig(config: ProxyConfig) {
    val host = config.host.trim()
    val port = config.port
    if (host.isNotEmpty() && port > 0) {
        runCatching {
            val address = InetSocketAddress(host, port)
            proxy(Proxy(Proxy.Type.HTTP, address))
        }.onFailure { err ->
            lgr.warn(err) { "Failed to apply configured proxy $host:$port, falling back to default selector" }
            ProxySelector.getDefault()?.let { proxySelector(it) }
        }
    } else {
        ProxySelector.getDefault()?.let { proxySelector(it) }
    }
}