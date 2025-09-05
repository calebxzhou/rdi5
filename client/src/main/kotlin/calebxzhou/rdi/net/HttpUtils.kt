package calebxzhou.rdi.net

import calebxzhou.rdi.Const
import calebxzhou.rdi.lgr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Duration

/**
 * calebxzhou @ 2025-05-10 23:20
 */

const val WEB_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:143.0) Gecko/20100101 Firefox/143.0"
// Helper function to force restricted headers using reflection
private fun HttpRequest.Builder.forceHeader(name: String, value: String): HttpRequest.Builder {
    return try {
        // First try normal way
        this.header(name, value)
    } catch (e: IllegalArgumentException) {
        try {
            // If that fails, use reflection to bypass restriction
            val builderClass = this.javaClass
            val headersField = builderClass.getDeclaredField("systemHeadersBuilder")
            headersField.isAccessible = true
            val headersBuilder = headersField.get(this)
            
            val addHeaderMethod = headersBuilder.javaClass.getDeclaredMethod("addHeader", String::class.java, String::class.java)
            addHeaderMethod.isAccessible = true
            addHeaderMethod.invoke(headersBuilder, name, value)
            
            if (Const.DEBUG) {
                lgr.info("Forced restricted header: $name = $value")
            }
            this
        } catch (reflectionException: Exception) {
            if (Const.DEBUG) {
                lgr.warn("Failed to force header $name = $value: ${reflectionException.message}")
            }
            this
        }
    }
}
val <T> HttpResponse<T>.success
    get() = this.statusCode() in 200..299
val HttpResponse<String>.body
    get() = this.body()
typealias StringHttpResponse = HttpResponse<String>
// Convenience function for String responses (most common case)
suspend fun httpStringRequest(
    post: Boolean = false,
    url: String,
    params: List<Pair<String, Any?>> = emptyList(),
    headers: List<Pair<String, String>> = emptyList()
): HttpResponse<String> = httpRequest<String>(post, url, params, headers)

// Generic function that chooses appropriate body handler based on type T
// Note: For file downloads, consider using downloadFile() function instead
suspend inline fun <reified T> httpRequest(
    post: Boolean = false,
    url: String,
    params: List<Pair<String, Any?>> = emptyList(),
    headers: List<Pair<String, String>> = emptyList()
): HttpResponse<T> = withContext(Dispatchers.IO) {
    System.setProperty("jdk.httpclient.allowRestrictedHeaders", "host,connection,content-length,expect,upgrade,via")
    // Filter out parameters with null values
    val filteredParams = params.filter { it.second != null }

    // Debug logging
    if (Const.DEBUG) {
        lgr.info("http ${if (post) "post" else "get"} $url ${filteredParams.joinToString(",")}")
        lgr.info("headers: ${headers.joinToString(",")}")
    }

    // Create JDK HTTP client
    val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build()

    val requestBuilder = if (post) {
        // For POST, create form data body
        val formData = filteredParams.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, StandardCharsets.UTF_8)}=${URLEncoder.encode(value.toString(), StandardCharsets.UTF_8)}"
        }
        
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .POST(HttpRequest.BodyPublishers.ofString(formData, StandardCharsets.UTF_8))
    } else {
        // For GET, add URL parameters
        val urlWithParams = if (filteredParams.isNotEmpty()) {
            val queryString = filteredParams.joinToString("&") { (key, value) ->
                "${URLEncoder.encode(key, StandardCharsets.UTF_8)}=${URLEncoder.encode(value.toString(), StandardCharsets.UTF_8)}"
            }
            "$url?$queryString"
        } else {
            url
        }
        
        HttpRequest.newBuilder()
            .uri(URI.create(urlWithParams))
            .GET()
    }

    // Add custom headers
    headers.forEach { (key, value) ->
        requestBuilder.header(key, value)
    }

    // Set timeout and build request
    val request = requestBuilder
        .timeout(Duration.ofSeconds(60))
        .build()

    // Choose appropriate body handler based on type T
    val bodyHandler = when (T::class) {
        String::class -> HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        ByteArray::class -> HttpResponse.BodyHandlers.ofByteArray()
        java.io.InputStream::class -> HttpResponse.BodyHandlers.ofInputStream()
        java.nio.file.Path::class -> HttpResponse.BodyHandlers.discarding() // For HEAD requests or when you don't need body
        else -> HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8) // Default to String
    } as HttpResponse.BodyHandler<T>

    // Send request and return response
    client.send(request, bodyHandler)
}

// Specialized function for downloading files directly to disk
suspend fun downloadFile(
    url: String,
    targetPath: Path,
    headers: List<Pair<String, String>> = emptyList(),
    overwrite: Boolean = true
): HttpResponse<Path> = withContext(Dispatchers.IO) {
    // Debug logging
    if (Const.DEBUG) {
        lgr.info("downloading file from $url to $targetPath")
    }

    // Create JDK HTTP client
    val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build()

    val requestBuilder = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .GET()

    // Add custom headers
    headers.forEach { (key, value) ->
        requestBuilder.header(key, value)
    }

    // Set timeout and build request
    val request = requestBuilder
        .timeout(Duration.ofSeconds(300)) // Longer timeout for file downloads
        .build()

    // Use ofFile body handler for efficient file download
    val bodyHandler = if (overwrite) {
        HttpResponse.BodyHandlers.ofFile(targetPath)
    } else {
        HttpResponse.BodyHandlers.ofFile(targetPath, StandardOpenOption.CREATE_NEW)
    }

    // Send request and return response
    client.send(request, bodyHandler)
}

suspend fun downloadFileWithProgress(
    url: String,
    targetPath: Path,
    onProgress: (bytesDownloaded: Long, totalBytes: Long, speed: Double) -> Unit
): Boolean = withContext(Dispatchers.IO) {
    try {
        val client = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(30))
            .build()

        val request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(url))
            .timeout(java.time.Duration.ofSeconds(300))
            .GET()
            .build()

        val response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofInputStream())

        if (response.statusCode() !in 200..299) {
            return@withContext false
        }

        val totalBytes = response.headers().firstValue("content-length")
            .map { it.toLongOrNull() ?: -1L }.orElse(-1L)

        response.body().use { inputStream ->
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
                    // Update progress every 500ms to avoid too frequent UI updates
                    if (currentTime - lastUpdateTime >= 500) {
                        val elapsedSeconds = (currentTime - startTime) / 1000.0
                        val speed = if (elapsedSeconds > 0) bytesDownloaded / elapsedSeconds else 0.0
                        onProgress(bytesDownloaded, totalBytes, speed)
                        lastUpdateTime = currentTime
                    }
                }

                // Final progress update
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
