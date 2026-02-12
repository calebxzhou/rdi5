package calebxzhou.rdi.common.net

import calebxzhou.mykotutils.log.Loggers
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.TimeUnit

/**
 * calebxzhou @ 2025-12-20 11:49
 * Simplified @ 2026-02-12
 */
data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val speedBytesPerSecond: Double,
) {
    val fraction: Float
        get() = if (totalBytes <= 0) -1f else (bytesDownloaded / totalBytes.toFloat()).coerceAtMost(1f)
}

private val lgr by Loggers

val httpFileClient by lazy {
    HttpClient(OkHttp) {
        expectSuccess = false
        engine {
            config {
                followRedirects(true)
                connectTimeout(15, TimeUnit.SECONDS)
                readTimeout(60, TimeUnit.SECONDS)
                retryOnConnectionFailure(true)
                dispatcher(Dispatcher().apply {
                    maxRequests = 1024
                    maxRequestsPerHost = 1024
                })
                connectionPool(ConnectionPool(1024, 1, TimeUnit.MINUTES))
                proxySelector(DynamicProxySelector())
            }
        }
        BrowserUserAgent()
        install(HttpTimeout) {
            requestTimeoutMillis = Long.MAX_VALUE
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 60_000
        }
    }
}

suspend fun Path.downloadFileFrom(
    url: String,
    headers: Map<String, String> = emptyMap(),
    knownSize: Long = 0L,
    onProgress: (DownloadProgress) -> Unit
): Result<Path> {
    lgr.info { "Start download file: $url -> $this" }
    if (url.isEmpty()) throw IllegalArgumentException("下载链接为空 无法下载到${this}")

    val targetPath = this

    // Ensure parent dir exists
    targetPath.parent?.let { parent ->
        withContext(Dispatchers.IO) {
            if (!Files.exists(parent)) Files.createDirectories(parent)
        }
    }

    return runCatching {
        downloadSingleStream(url, targetPath, onProgress, knownSize,headers)
        this
    }
}

private suspend fun downloadSingleStream(
    url: String,
    targetPath: Path,
    onProgress: (DownloadProgress) -> Unit,
    knownSize: Long = -1L,
    headers: Map<String, String>
): Boolean {
    return httpFileClient.prepareGet(url) {
        headers.forEach { (key, value) -> header(key, value) }
        header(HttpHeaders.AcceptEncoding, "identity")
    }.execute { response ->
        if (!response.status.isSuccess()) {
            throw IOException("Download failed: ${response.status} for ${url}")
        }

        val totalBytes = response.contentLength() ?: knownSize

        val channel: ByteReadChannel = response.body()
        val buffer = ByteArray(8192)
        var bytesDownloaded = 0L

        var lastReportTime = System.currentTimeMillis()
        var lastReportBytes = 0L

        var consecutiveZeroReads = 0
        val maxConsecutiveZeroReads = 100

        withContext(Dispatchers.IO) {
            Files.newOutputStream(targetPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
                .use { outputStream ->
                    while (!channel.isClosedForRead) {
                        val bytesRead = withTimeoutOrNull(30_000L) {
                            channel.readAvailable(buffer, 0, buffer.size)
                        } ?: throw IOException("Read timeout - connection stalled")

                        if (bytesRead == -1) break

                        if (bytesRead == 0) {
                            consecutiveZeroReads++
                            if (consecutiveZeroReads >= maxConsecutiveZeroReads) {
                                throw IOException("Connection stalled - too many zero reads")
                            }
                            delay(100)
                            continue
                        }
                        consecutiveZeroReads = 0

                        outputStream.write(buffer, 0, bytesRead)
                        bytesDownloaded += bytesRead

                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastReportTime >= 500) {
                            val timeDelta = (currentTime - lastReportTime) / 1000.0
                            val bytesDelta = bytesDownloaded - lastReportBytes
                            val speed = if (timeDelta > 0) bytesDelta / timeDelta else 0.0

                            onProgress(DownloadProgress(bytesDownloaded, totalBytes, speed))

                            lastReportTime = currentTime
                            lastReportBytes = bytesDownloaded
                        }
                    }
                    onProgress(DownloadProgress(bytesDownloaded, totalBytes, 0.0))
                }
        }
        true
    }
}
