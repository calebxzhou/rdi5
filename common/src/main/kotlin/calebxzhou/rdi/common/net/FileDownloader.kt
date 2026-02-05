package calebxzhou.rdi.common.net

import calebxzhou.mykotutils.log.Loggers
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.ceil
import kotlin.math.max

/**
 * calebxzhou @ 2025-12-20 11:49
 * Fixed @ 2026-01-01
 */
data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val speedBytesPerSecond: Double,
) {
    val fraction: Float
        get() = if (totalBytes <= 0) -1f else (bytesDownloaded / totalBytes.toFloat() ).coerceAtMost(1f)
}

private data class RemoteFileInfo(val contentLength: Long, val acceptRanges: Boolean)

private val lgr by Loggers
private const val MIN_PARALLEL_DOWNLOAD_SIZE = 5L * 1024 * 1024 // Increased to 5MB to avoid overhead on small files
private const val TARGET_RANGE_CHUNK_SIZE = 4L * 1024 * 1024 // 4MB per chunk
val DEFAULT_RANGE_PARALLELISM = max(2, Runtime.getRuntime().availableProcessors())


// BUG FIX 1: Use 'by lazy' or direct assignment. Do NOT use 'get() =' which creates a new client every call.
val httpFileClient by lazy {
    HttpClient(OkHttp) {
        expectSuccess = false
        engine {
            config {
                followRedirects(true)
                connectTimeout(15, TimeUnit.SECONDS)
                // BUG FIX: Set a reasonable read timeout instead of infinite (0)
                // This prevents hangs when server stops sending data but keeps connection open
                readTimeout(60, TimeUnit.SECONDS)
                retryOnConnectionFailure(true)
                // Increase parallelism and connection pool for asset swarms
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
            requestTimeoutMillis = Long.MAX_VALUE // Handle large file timeouts manually via socket
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 60_000
        }
    }
}

suspend fun Path.downloadFileFrom(
    url: String,
    headers: Map<String, String> = emptyMap(),
    // FIX 2: Add parameter to pass known file size
    knownSize: Long = -1L,
    rangeParallelism: Int = DEFAULT_RANGE_PARALLELISM,
    onProgress: (DownloadProgress) -> Unit
): Result<Path> {
    lgr.info { "Start download file: $url -> $this" }
    if(url.isEmpty()) throw IllegalArgumentException("下载链接为空 无法下载到${this}")
    val targetPath = this

    // FIX 3: Optimization strategy
    // If we know the size is small, SKIP the HEAD request and go straight to download
    if (knownSize in 1 until MIN_PARALLEL_DOWNLOAD_SIZE) {
        return runCatching {
            downloadSingleStream(url, targetPath, onProgress, headers, expectedTotal = knownSize)
            this
        }
    }

    // Logic for unknown size or large files (keep existing logic)
    val info = fetchRemoteFileInfo(url, headers)
    val totalSize = info?.contentLength ?: knownSize
    // Ensure parent dir exists
    targetPath.parent?.let { parent ->
        withContext(Dispatchers.IO) {
            if (!Files.exists(parent)) Files.createDirectories(parent)
        }
    }
    val canUseRanges = info?.let { it.acceptRanges && it.contentLength >= MIN_PARALLEL_DOWNLOAD_SIZE } == true

    if (canUseRanges) {
        runCatching {
            downloadWithRanges(url, targetPath, totalSize, onProgress, headers, rangeParallelism)
        }.getOrElse { error ->
            lgr.warn(error) { "Parallel download failed, falling back to single stream" }
            // Clean up potentially corrupted partial file before fallback
            withContext(Dispatchers.IO) { Files.deleteIfExists(targetPath) }
            downloadSingleStream(url, targetPath, onProgress, headers, expectedTotal = totalSize)
        }
    } else {
        downloadSingleStream(url, targetPath, onProgress, headers, expectedTotal = totalSize)
    }
    return Result.success(this)
}

private suspend fun fetchRemoteFileInfo(url: String, headers: Map<String, String>): RemoteFileInfo? = try {
    val response = httpFileClient.head(url) {
        headers.forEach { (key, value) -> header(key, value) }
        timeout {
            requestTimeoutMillis = 10_000
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
    lgr.warn { "HEAD request failed for $url: ${e.message}" }
    null
}

private suspend fun downloadSingleStream(
    url: String,
    targetPath: Path,
    onProgress: (DownloadProgress) -> Unit,
    headers: Map<String, String>,
    expectedTotal: Long = -1L
): Boolean {
    return httpFileClient.prepareGet(url) {
        headers.forEach { (key, value) -> header(key, value) }
        header(HttpHeaders.AcceptEncoding, "identity") // Force no compression to get accurate byte counts
    }.execute { response ->
        if (!response.status.isSuccess()) {
            throw IOException("Download failed: ${response.status} for ${url}")
        }

        val totalBytes = if (expectedTotal > 0) expectedTotal else response.contentLength() ?: -1L

        val channel: ByteReadChannel = response.body()
        val buffer = ByteArray(8192)
        var bytesDownloaded = 0L

        // Progress tracking (Instant speed)
        var lastReportTime = System.currentTimeMillis()
        var lastReportBytes = 0L
        
        // BUG FIX: Track consecutive zero reads to detect stalled connections
        var consecutiveZeroReads = 0
        val maxConsecutiveZeroReads = 100 // ~10 seconds with 100ms delay

        withContext(Dispatchers.IO) {
            Files.newOutputStream(targetPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
                .use { outputStream ->
                    while (!channel.isClosedForRead) {
                        // BUG FIX: Use withTimeout to prevent indefinite hangs on read
                        val bytesRead = withTimeoutOrNull(30_000L) {
                            channel.readAvailable(buffer, 0, buffer.size)
                        } ?: throw IOException("Read timeout - connection stalled")
                        
                        if (bytesRead == -1) break
                        
                        // BUG FIX: Handle zero reads properly - yield and track consecutive zeros
                        if (bytesRead == 0) {
                            consecutiveZeroReads++
                            if (consecutiveZeroReads >= maxConsecutiveZeroReads) {
                                throw IOException("Connection stalled - too many zero reads")
                            }
                            delay(100) // Prevent busy-wait, give network time
                            continue
                        }
                        consecutiveZeroReads = 0 // Reset on successful read

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
                    // Final report
                    onProgress(DownloadProgress(bytesDownloaded, totalBytes, 0.0))
                }
        }
        true
    }
}

private suspend fun downloadWithRanges(
    url: String,
    targetPath: Path,
    totalBytes: Long,
    onProgress: (DownloadProgress) -> Unit,
    headers: Map<String, String>,
    rangeParallelism: Int
): Boolean {
    val chunkCount = max(1, ceil(totalBytes.toDouble() / TARGET_RANGE_CHUNK_SIZE).toInt())
    val ranges = buildRanges(totalBytes, chunkCount)

    // Throttle parallelism
    val semaphore = Semaphore(max(1, rangeParallelism))
    val progressUpdater = createProgressAggregator(totalBytes, onProgress)

    return withContext(Dispatchers.IO) {
        FileChannel.open(
            targetPath,
            StandardOpenOption.WRITE,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING
        ).use { fileChannel ->
            coroutineScope {
                ranges.map { (start, end) ->
                    async {
                        // Retry logic per chunk
                        var attempt = 0
                        while (attempt < 3) {
                            try {
                                semaphore.withPermit {
                                    downloadRangeChunk(url, start, end, totalBytes, fileChannel, progressUpdater, headers)
                                }
                                return@async // Success
                            } catch (e: Exception) {
                                attempt++
                                if (attempt >= 3) throw e
                                delay(1000L * attempt) // Exponential backoff
                            }
                        }
                    }
                }.awaitAll()
            }
            true
        }
    }.also {
        progressUpdater(0, true) // Final update
    }
}

private fun buildRanges(totalBytes: Long, chunkCount: Int): List<Pair<Long, Long>> {
    if (totalBytes <= 0) return listOf(0L to -1L)
    val ranges = mutableListOf<Pair<Long, Long>>()
    val partSize = totalBytes / chunkCount
    var start = 0L

    for (i in 0 until chunkCount) {
        // Last chunk gets the remainder
        val end = if (i == chunkCount - 1) totalBytes - 1 else start + partSize - 1
        ranges.add(start to end)
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
    progressUpdater: (Long, Boolean) -> Unit,
    headers: Map<String, String>
) {
    val response = httpFileClient.get(url) {
        headers.forEach { (key, value) -> header(key, value) }
        header(HttpHeaders.Range, "bytes=$start-$end")
        header(HttpHeaders.AcceptEncoding, "identity")
    }

    // BUG FIX 2: Explicitly ensure we got Partial Content.
    // If server ignores range and returns 200 OK, we must fail range download to avoid corrupting file.
    if (response.status != HttpStatusCode.PartialContent) {
        throw IllegalStateException("Server returned ${response.status} instead of 206 Partial Content for range $start-$end")
    }

    val channel: ByteReadChannel = response.body()
    // Allocate buffer once, wrap inside loop to update position
    val buffer = ByteArray(16 * 1024)
    var position = start
    
    // BUG FIX: Calculate expected bytes for this range and track progress
    val expectedBytes = end - start + 1
    var bytesReceived = 0L
    
    // BUG FIX: Track consecutive zero reads to detect stalled connections
    var consecutiveZeroReads = 0
    val maxConsecutiveZeroReads = 100 // ~10 seconds with 100ms delay

    while (!channel.isClosedForRead) {
        // BUG FIX: Exit when we've received all expected bytes (don't rely solely on isClosedForRead)
        if (bytesReceived >= expectedBytes) {
            break
        }
        
        // BUG FIX: Use withTimeout to prevent indefinite hangs on read
        val bytesRead = withTimeoutOrNull(30_000L) {
            channel.readAvailable(buffer, 0, buffer.size)
        } ?: throw IOException("Read timeout - connection stalled for range $start-$end")
        
        if (bytesRead == -1) break
        
        // BUG FIX: Handle zero reads properly - yield and track consecutive zeros
        if (bytesRead == 0) {
            consecutiveZeroReads++
            if (consecutiveZeroReads >= maxConsecutiveZeroReads) {
                throw IOException("Connection stalled - too many zero reads for range $start-$end")
            }
            delay(100) // Prevent busy-wait, give network time
            continue
        }
        consecutiveZeroReads = 0 // Reset on successful read

        // Write to specific position in file
        writeBuffer(fileChannel, buffer, bytesRead, position)
        position += bytesRead
        bytesReceived += bytesRead
        progressUpdater(bytesRead.toLong(), false)
    }
    
    // BUG FIX: Verify we received all expected bytes
    if (bytesReceived < expectedBytes) {
        throw IOException("Incomplete range download: expected $expectedBytes bytes but received $bytesReceived for range $start-$end")
    }
}

private fun writeBuffer(channel: FileChannel, buffer: ByteArray, length: Int, position: Long) {
    val byteBuffer = ByteBuffer.wrap(buffer, 0, length)
    var writtenTotal = 0
    while (byteBuffer.hasRemaining()) {
        val written = channel.write(byteBuffer, position + writtenTotal)
        writtenTotal += written
    }
}

private fun createProgressAggregator(
    totalBytes: Long,
    onProgress: (DownloadProgress) -> Unit
): (Long, Boolean) -> Unit {
    val downloadedTotal = AtomicLong(0)

    // Speed calculation state
    val lastUpdateTimestamp = AtomicLong(System.currentTimeMillis())
    val lastDownloadedSnapshot = AtomicLong(0)

    return prog@{ deltaBytes, force ->
        val currentTotal = if (deltaBytes > 0) downloadedTotal.addAndGet(deltaBytes) else downloadedTotal.get()
        val now = System.currentTimeMillis()
        val lastTime = lastUpdateTimestamp.get()

        if (!force && (now - lastTime < 500)) {
            return@prog
        }

        // CAS loop to ensure thread-safe updates of the timestamp for speed calculation
        if (lastUpdateTimestamp.compareAndSet(lastTime, now)) {
            val lastBytes = lastDownloadedSnapshot.getAndSet(currentTotal)

            val timeDeltaSec = (now - lastTime) / 1000.0
            val bytesDelta = currentTotal - lastBytes

            // BUG FIX 3: Calculate instant speed, not average speed
            val speed = if (timeDeltaSec > 0) bytesDelta / timeDeltaSec else 0.0

            onProgress(DownloadProgress(currentTotal, totalBytes, speed))
        }
    }
}
