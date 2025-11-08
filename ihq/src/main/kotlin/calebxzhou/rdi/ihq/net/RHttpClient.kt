package calebxzhou.rdi.ihq.net

import calebxzhou.rdi.ihq.CONF
import calebxzhou.rdi.ihq.lgr
import calebxzhou.rdi.ihq.util.serdesJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.BrowserUserAgent
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.jvm.javaio.toInputStream
import io.netty.handler.codec.compression.StandardCompressionOptions.deflate
import io.netty.handler.codec.compression.StandardCompressionOptions.gzip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.TimeUnit
import kotlin.text.toLongOrNull

/**
 * calebxzhou @ 2025-10-16 20:42
 */
const val WEB_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:143.0) Gecko/20100101 Firefox/143.0"
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
                    applyProxyConfig()
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
data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val speedBytesPerSecond: Double,
){
    val percent: Double
        get() = if (totalBytes <= 0) -1.0 else bytesDownloaded.toDouble() / totalBytes.toDouble() * 100.0
}
suspend fun downloadFileWithProgress(
    url: String,
    targetPath: Path,
    onProgress: (DownloadProgress) -> Unit
): Boolean = withContext(Dispatchers.IO) {
    try {
        val response = ktorClient.get(url) {
            header(HttpHeaders.UserAgent, WEB_USER_AGENT)
            timeout {
                requestTimeoutMillis = 300_000
                socketTimeoutMillis = 300_000
            }
        }

        if (response.status.value !in 200..299) {
            return@withContext false
        }

        val totalBytes = response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: -1L
        targetPath.parent?.let { parent ->
            if (!Files.exists(parent)) {
                Files.createDirectories(parent)
            }
        }

        response.bodyAsChannel().toInputStream().use { inputStream ->
            Files.newOutputStream(targetPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).use { outputStream ->
                val buffer = ByteArray(8192)
                var bytesDownloaded = 0L
                val startTime = System.currentTimeMillis()
                var lastUpdateTime = startTime

                while (true) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead == -1) break

                    outputStream.write(buffer, 0, bytesRead)
                    bytesDownloaded += bytesRead

                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastUpdateTime >= 500) {
                        val elapsedSeconds = (currentTime - startTime) / 1000.0
                        val speed = if (elapsedSeconds > 0) bytesDownloaded / elapsedSeconds else 0.0
                        onProgress(DownloadProgress(bytesDownloaded, totalBytes, speed))
                        lastUpdateTime = currentTime
                    }
                }

                val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0
                val speed = if (elapsedSeconds > 0) bytesDownloaded / elapsedSeconds else 0.0
                onProgress(DownloadProgress(bytesDownloaded, totalBytes, speed))
            }
        }
        true
    } catch (e: Exception) {
        lgr.error("Download failed for $url", e)
        false
    }
}

private fun okhttp3.OkHttpClient.Builder.applyProxyConfig() {
    val proxyConfig = CONF.proxy
    val host = proxyConfig.host.trim()
    val port = proxyConfig.port
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