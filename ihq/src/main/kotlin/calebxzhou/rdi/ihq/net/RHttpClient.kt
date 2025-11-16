package calebxzhou.rdi.ihq.net

import calebxzhou.rdi.ihq.CONF
import calebxzhou.rdi.ihq.ProxyConfig
import calebxzhou.rdi.ihq.lgr
import calebxzhou.rdi.ihq.util.serdesJson
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.channels.FileChannel
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * calebxzhou @ 2025-10-16 20:42
 */

suspend inline fun httpRequest(crossinline builder: HttpRequestBuilder.() -> Unit): HttpResponse = ktorClient.request(builder)

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
data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val speedBytesPerSecond: Double,
){
    val percent: Double
        get() = if (totalBytes <= 0) -1.0 else bytesDownloaded.toDouble() / totalBytes.toDouble() * 100.0
}

private data class RemoteFileInfo(val contentLength: Long, val acceptRanges: Boolean)

private const val MIN_PARALLEL_DOWNLOAD_SIZE = 2L * 1024 * 1024 // 2MB
private const val TARGET_RANGE_CHUNK_SIZE = 2L * 1024 * 1024 // 2MB per chunk
private val MAX_PARALLEL_RANGE_REQUESTS = max(2, Runtime.getRuntime().availableProcessors())

suspend fun downloadFileWithProgress(
    url: String,
    targetPath: Path,
    onProgress: (DownloadProgress) -> Unit
): Boolean {
    lgr.info { "try download $url" }
    return try {
        val info = fetchRemoteFileInfo(url)
        val canUseRanges = info?.let { it.acceptRanges && it.contentLength >= MIN_PARALLEL_DOWNLOAD_SIZE } == true
        if (canUseRanges) {
            runCatching {
                downloadWithRanges(url, targetPath, info!!.contentLength, onProgress)
            }.getOrElse { error ->
                lgr.warn(error) { "Parallel download failed, falling back to single stream" }
                downloadSingleStream(url, targetPath, onProgress)
            }
        } else {
            downloadSingleStream(url, targetPath, onProgress)
        }
    } catch (e: Exception) {
        lgr.error("Download failed for $url", e)
        false
    }
}

private suspend fun fetchRemoteFileInfo(url: String): RemoteFileInfo? = try {
    val response = ktorClient.request {
        method = HttpMethod.Head
        url(url)
        header(HttpHeaders.UserAgent, WEB_USER_AGENT)
        timeout {
            requestTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
        }
    }
    if (!response.status.isSuccess()) {
        null
    } else {
        val length = response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: -1L
        val acceptsRanges = response.headers[HttpHeaders.AcceptRanges]?.contains("bytes", ignoreCase = true) == true
        RemoteFileInfo(length, acceptsRanges)
    }
} catch (e: Exception) {
    lgr.warn(e) { "HEAD request failed for $url" }
    null
}

private suspend fun downloadSingleStream(
    url: String,
    targetPath: Path,
    onProgress: (DownloadProgress) -> Unit
): Boolean {
    return ktorClient.prepareGet(url) {
        header(HttpHeaders.UserAgent, WEB_USER_AGENT)
        timeout {
            requestTimeoutMillis = 600_000
            socketTimeoutMillis = 600_000
        }
    }.execute { response ->
        if (!response.status.isSuccess()) {
            return@execute false
        }

        val totalBytes = response.contentLength() ?: response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: -1L
        targetPath.parent?.let { parent ->
            withContext(Dispatchers.IO) {
                if (!Files.exists(parent)) {
                    Files.createDirectories(parent)
                }
            }
        }

        val channel: ByteReadChannel = response.body()
        val buffer = ByteArray(8192)
        var bytesDownloaded = 0L
        val startTime = System.currentTimeMillis()
        var lastUpdateTime = startTime

        withContext(Dispatchers.IO) {
            Files.newOutputStream(targetPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).use { outputStream ->
                while (!channel.isClosedForRead) {
                    val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
                    if (bytesRead == -1) break
                    if (bytesRead == 0) continue

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
    }
}

private suspend fun downloadWithRanges(
    url: String,
    targetPath: Path,
    totalBytes: Long,
    onProgress: (DownloadProgress) -> Unit
): Boolean {
    targetPath.parent?.let { parent ->
        withContext(Dispatchers.IO) {
            if (!Files.exists(parent)) {
                Files.createDirectories(parent)
            }
        }
    }

    val chunkCount = max(1, ceil(totalBytes.toDouble() / TARGET_RANGE_CHUNK_SIZE).toInt())
    val ranges = buildRanges(totalBytes, chunkCount)
    val parallelism = min(MAX_PARALLEL_RANGE_REQUESTS, ranges.size)
    val progressUpdater = createProgressAggregator(totalBytes, onProgress)

    return withContext(Dispatchers.IO) {
        FileChannel.open(
            targetPath,
            StandardOpenOption.WRITE,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        ).use { fileChannel ->
            coroutineScope {
                val semaphore = Semaphore(parallelism)
                val jobs = ranges.map { (start, end) ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            downloadRangeChunk(url, start, end, totalBytes, fileChannel, progressUpdater)
                        }
                    }
                }
                jobs.awaitAll()
            }
            true
        }
    }.also {
        progressUpdater(0, true)
    }
}

private fun buildRanges(totalBytes: Long, chunkCount: Int): List<Pair<Long, Long>> {
    if (totalBytes <= 0) return listOf(0L to -1L)
    val chunkSize = max(1L, totalBytes / chunkCount)
    val ranges = mutableListOf<Pair<Long, Long>>()
    var start = 0L
    while (start < totalBytes) {
        val end = min(totalBytes - 1, start + chunkSize - 1)
        ranges += start to end
        start = end + 1
    }
    return ranges
}

private suspend fun downloadRangeChunk(
    url: String,
    start: Long,
    end: Long,
    totalBytes: Long,
    fileChannel: FileChannel,
    progressUpdater: (Long, Boolean) -> Unit
) {
    val response = ktorClient.get(url) {
        header(HttpHeaders.UserAgent, WEB_USER_AGENT)
        header(HttpHeaders.Range, "bytes=$start-$end")
        header(HttpHeaders.AcceptEncoding, "identity")
        timeout {
            requestTimeoutMillis = 600_000
            socketTimeoutMillis = 600_000
        }
    }

    if (start != 0L || (end != totalBytes - 1)) {
        if (response.status != HttpStatusCode.PartialContent) {
            throw IllegalStateException("Server did not honor range request, status=${response.status.value}")
        }
    }

    val channel: ByteReadChannel = response.body()
    val buffer = ByteArray(8192)
    var position = start
    while (!channel.isClosedForRead) {
        val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
        if (bytesRead == -1) break
        if (bytesRead == 0) continue

        writeBuffer(fileChannel, buffer, bytesRead, position)
        position += bytesRead
        progressUpdater(bytesRead.toLong(), false)
    }
}

private fun writeBuffer(channel: FileChannel, buffer: ByteArray, length: Int, position: Long) {
    var written = 0
    while (written < length) {
        val byteBuffer = ByteBuffer.wrap(buffer, written, length - written)
        val bytes = channel.write(byteBuffer, position + written)
        written += bytes
    }
}

private fun createProgressAggregator(
    totalBytes: Long,
    onProgress: (DownloadProgress) -> Unit
): (Long, Boolean) -> Unit {
    val downloaded = AtomicLong(0)
    val startTime = System.currentTimeMillis()
    val lastUpdate = AtomicLong(startTime)

    return prog@{ delta, force ->
        if (delta != 0L) {
            downloaded.addAndGet(delta)
        }
        val now = System.currentTimeMillis()
        if (!force) {
            val last = lastUpdate.get()
            if (now - last < 500) {
                return@prog
            }
        }
        lastUpdate.set(now)
        val elapsedSeconds = ((now - startTime) / 1000.0).coerceAtLeast(0.001)
        val total = downloaded.get()
        val speed = if (elapsedSeconds > 0) total / elapsedSeconds else 0.0
        onProgress(DownloadProgress(total, totalBytes, speed))
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