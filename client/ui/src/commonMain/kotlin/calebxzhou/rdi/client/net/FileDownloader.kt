package calebxzhou.rdi.client.net

import calebxzhou.mykotutils.log.Loggers
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import calebxzhou.rdi.common.net.ktorClient

/**
 * Multiplatform file downloader for both PC and Android
 */
data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val speedBytesPerSecond: Double,
) {
    val fraction: Float
        get() = if (totalBytes <= 0) -1f else (bytesDownloaded / totalBytes.toFloat()).coerceAtMost(1f)
}


/**
 * Platform-specific: write bytes to a file at the given path.
 * On desktop, uses java.nio.file; on Android, uses standard java.io.
 * @param filePath the target file path string
 * @param bytes the bytes to write
 * @param append whether to append or overwrite
 */
expect suspend fun writeFileBytes(filePath: String, bytes: ByteArray, append: Boolean = false)

/**
 * Platform-specific: ensure parent directories exist.
 */
expect suspend fun ensureParentDirs(filePath: String)

/**
 * Download a file from URL to the given file path with progress reporting.
 */
suspend fun downloadFileTo(
    url: String,
    filePath: String,
    headers: Map<String, String> = emptyMap(),
    knownSize: Long = 0L,
    onProgress: (DownloadProgress) -> Unit
): Result<String> {
    lgr.info { "Start download: $url -> $filePath" }
    if (url.isEmpty()) throw IllegalArgumentException("下载链接为空 无法下载到$filePath")

    ensureParentDirs(filePath)

    return runCatching {
        // Clear file first
        writeFileBytes(filePath, ByteArray(0), append = false)

        ktorClient.prepareGet(url) {
            headers.forEach { (key, value) -> header(key, value) }
            header(HttpHeaders.AcceptEncoding, "identity")
            timeout {
                requestTimeoutMillis = Long.MAX_VALUE
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 60_000
            }
        }.execute { response ->
            if (!response.status.isSuccess()) {
                throw Exception("Download failed: ${response.status} for $url")
            }

            val totalBytes = response.contentLength() ?: knownSize
            val channel: ByteReadChannel = response.body()
            val buffer = ByteArray(8192)
            var bytesDownloaded = 0L

            var lastReportTime = System.currentTimeMillis()
            var lastReportBytes = 0L

            var consecutiveZeroReads = 0
            val maxConsecutiveZeroReads = 100

            while (!channel.isClosedForRead) {
                val bytesRead = withTimeoutOrNull(30_000L) {
                    channel.readAvailable(buffer, 0, buffer.size)
                } ?: throw Exception("Read timeout - connection stalled")

                if (bytesRead == -1) break

                if (bytesRead == 0) {
                    consecutiveZeroReads++
                    if (consecutiveZeroReads >= maxConsecutiveZeroReads) {
                        throw Exception("Connection stalled - too many zero reads")
                    }
                    delay(100)
                    continue
                }
                consecutiveZeroReads = 0

                writeFileBytes(filePath, buffer.copyOf(bytesRead), append = true)
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
        filePath
    }
}
