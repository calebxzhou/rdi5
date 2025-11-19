package calebxzhou.rdi.net

import calebxzhou.rdi.lgr
import calebxzhou.rdi.model.RAccount
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

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

suspend fun downloadFileWithProgress(
    url: String,
    targetPath: Path,
    onProgress: (bytesDownloaded: Long, totalBytes: Long, speed: Double) -> Unit
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
                        onProgress(bytesDownloaded, totalBytes, speed)
                        lastUpdateTime = currentTime
                    }
                }

                val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000.0
                val speed = if (elapsedSeconds > 0) bytesDownloaded / elapsedSeconds else 0.0
                onProgress(bytesDownloaded, totalBytes, speed)
            }
        }
        true
    } catch (e: Exception) {
        lgr.error("Download failed for $url", e)
        false
    }
}
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
