package calebxzhou.rdi.net

import calebxzhou.rdi.lgr
import calebxzhou.rdi.model.RAccount
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.http.HttpResponse
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * calebxzhou @ 2025-05-10 23:20
 */

const val WEB_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:143.0) Gecko/20100101 Firefox/143.0"



val HttpResponse<String>.body
    get() = this.body()

fun HttpRequestBuilder.accountAuthHeader(){
    RAccount.now?.let {
        header(HttpHeaders.Authorization, "Bearer ${it.jwt}")
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
    lgr.info ( "try download $url" )
    return try {
        val info = fetchRemoteFileInfo(url)
        val canUseRanges = info?.let { it.acceptRanges && it.contentLength >= MIN_PARALLEL_DOWNLOAD_SIZE } == true
        if (canUseRanges) {
            runCatching {
                downloadWithRanges(url, targetPath, info!!.contentLength, onProgress)
            }.getOrElse { error ->
                lgr.warn( "Parallel download failed, falling back to single stream" )
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
    lgr.warn( "HEAD request failed for $url" )
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
    val response =  ktorClient.get(url) {
        header(HttpHeaders.UserAgent,  WEB_USER_AGENT)
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
/*
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
}*/
// Download file with progress tracking

// Format bytes to human readable format
fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "${bytes}B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1fKB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1fMB".format(mb)
    val gb = mb / 1024.0
    return "%.1fGB".format(gb)
}
val Long.humanSize: String
    get() = formatBytes(this)
// Format speed to human readable format
fun formatSpeed(bytesPerSecond: Double): String {
    if (bytesPerSecond < 1024) return "%.0fB/s".format(bytesPerSecond)
    val kbps = bytesPerSecond / 1024.0
    if (kbps < 1024) return "%.1fKB/s".format(kbps)
    val mbps = kbps / 1024.0
    if (mbps < 1024) return "%.1fMB/s".format(mbps)
    val gbps = mbps / 1024.0
    return "%.1fGB/s".format(gbps)
}
