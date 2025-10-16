package calebxzhou.rdi.net

import calebxzhou.rdi.Const
import calebxzhou.rdi.lgr
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse as KtorHttpResponse
import io.ktor.client.statement.*
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.contentType
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.ProxySelector
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.util.Optional
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSession
import kotlin.LazyThreadSafetyMode
import kotlin.text.Charsets
import java.net.http.HttpClient as JdkHttpClient

/**
 * calebxzhou @ 2025-05-10 23:20
 */

const val WEB_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:143.0) Gecko/20100101 Firefox/143.0"

@PublishedApi
internal val ktorHttpClient: HttpClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    HttpClient(OkHttp) {
        expectSuccess = false
        engine {
            config {
                followRedirects(true)
                connectTimeout(10, TimeUnit.SECONDS)
                readTimeout(0, TimeUnit.SECONDS)
                ProxySelector.getDefault()?.let { proxySelector(it) }
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 60_000
        }
    }
}

@PublishedApi
internal fun MutableMap<String, MutableList<String>>.recordHeader(name: String, value: String) {
    getOrPut(name) { mutableListOf() }.add(value)
}

@PublishedApi
internal fun Map<String, MutableList<String>>.containsHeader(name: String): Boolean =
    keys.any { it.equals(name, ignoreCase = true) }

@PublishedApi
internal fun Map<String, MutableList<String>>.firstHeaderValue(name: String): String? =
    entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value?.firstOrNull()

@PublishedApi
internal fun Headers.toJavaHeaders(): java.net.http.HttpHeaders =
    java.net.http.HttpHeaders.of(
        entries().associate { entry -> entry.key to entry.value },
        { _, _ -> true }
    )
val <T> HttpResponse<T>.success
    get() = this.statusCode() in 200..299
val HttpResponse<String>.body
    get() = this.body()
typealias StringHttpResponse = HttpResponse<String>

suspend fun httpStringRequest_(
    post: Boolean = false,
    url: String,
    params: List<Pair<String, Any?>> = emptyList(),
    headers: List<Pair<String, String>> = emptyList(),
    jsonBody: String? = null
): HttpResponse<String> = httpRequest_(post, url, params, headers, jsonBody)

suspend inline fun <reified T> httpRequest_(
    post: Boolean = false,
    url: String,
    params: List<Pair<String, Any?>> = emptyList(),
    headers: List<Pair<String, String>> = emptyList(),
    jsonBody: String? = null
): HttpResponse<T> = withContext(Dispatchers.IO) {
    val filteredParams = params.filter { it.second != null }

    val finalUrl = if (!post && filteredParams.isNotEmpty()) {
        val queryString = filteredParams.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, StandardCharsets.UTF_8)}=${URLEncoder.encode(value.toString(), StandardCharsets.UTF_8)}"
        }
        if (url.contains("?")) "$url&$queryString" else "$url?$queryString"
    } else {
        url
    }

    if (Const.DEBUG) {
        lgr.info("http ${if (post) "post" else "get"} $finalUrl ${filteredParams.joinToString(",")}")
        lgr.info("headers: ${headers.joinToString(",")}")
    }

    val requestHeaders = mutableMapOf<String, MutableList<String>>()
    requestHeaders.recordHeader(HttpHeaders.UserAgent, WEB_USER_AGENT)
    headers.forEach { (name, value) -> requestHeaders.recordHeader(name, value) }
    if (finalUrl.contains("api.curseforge.com") && !requestHeaders.containsHeader("x-api-key")) {
        requestHeaders.recordHeader("x-api-key", Const.CF_AKEY)
    }

    var requestBody: String? = null
    var requestBodyBytes: ByteArray? = null

    if (post) {
        if (jsonBody != null) {
            requestBody = jsonBody
            requestBodyBytes = jsonBody.toByteArray(StandardCharsets.UTF_8)
            if (!requestHeaders.containsHeader(HttpHeaders.ContentType)) {
                requestHeaders.recordHeader(HttpHeaders.ContentType, "application/json; charset=UTF-8")
            }
        } else {
            val formData = filteredParams.joinToString("&") { (key, value) ->
                "${URLEncoder.encode(key, StandardCharsets.UTF_8)}=${URLEncoder.encode(value.toString(), StandardCharsets.UTF_8)}"
            }
            requestBody = formData
            requestBodyBytes = formData.toByteArray(StandardCharsets.UTF_8)
            if (!requestHeaders.containsHeader(HttpHeaders.ContentType)) {
                requestHeaders.recordHeader(HttpHeaders.ContentType, "application/x-www-form-urlencoded; charset=UTF-8")
            }
        }
    }
    val ktorResponse: KtorHttpResponse = ktorHttpClient.request {
        url(finalUrl)
        method = if (post) HttpMethod.Post else HttpMethod.Get
        for ((name, values) in requestHeaders) {
            values.forEach { header(name, it) }
        }
        if (requestBody != null) {
            requestHeaders.firstHeaderValue(HttpHeaders.ContentType)
                ?.let { runCatching { ContentType.parse(it) }.getOrNull() }
                ?.let { contentType(it) }
            setBody(requestBody)
        }
        timeout {
            requestTimeoutMillis = 60_000
            socketTimeoutMillis = 60_000
        }
    }

    val responseBody: T = when (T::class) {
        String::class -> ktorResponse.bodyAsText(Charsets.UTF_8) as T
        ByteArray::class -> ktorResponse.bodyAsBytes() as T
        java.io.InputStream::class -> ktorResponse.bodyAsChannel().toInputStream() as T
        else -> ktorResponse.bodyAsText(Charsets.UTF_8) as T
    }

    val responseHeaders = ktorResponse.headers.toJavaHeaders()
    val requestUri = URI.create(ktorResponse.request.url.toString())
    val javaRequest = buildJavaRequest(requestUri, if (post) HttpMethod.Post.value else HttpMethod.Get.value, requestHeaders, requestBodyBytes)

    KtorHttpResponseAdapter(
        request = javaRequest,
        uri = requestUri,
        version = ktorResponse.version.toJavaVersion(),
        statusCode = ktorResponse.status.value,
        headers = responseHeaders,
        body = responseBody
    )
}

@PublishedApi
internal fun buildJavaRequest(
    uri: URI,
    method: String,
    headers: Map<String, MutableList<String>>,
    bodyBytes: ByteArray?
): HttpRequest {
    val builder = HttpRequest.newBuilder(uri)
        .timeout(Duration.ofSeconds(60))
    headers.forEach { (name, values) ->
        values.forEach { builder.header(name, it) }
    }
    val upperMethod = method.uppercase()
    return when {
        upperMethod == "GET" -> builder.GET().build()
        upperMethod == "POST" && bodyBytes != null -> builder.POST(BodyPublishers.ofByteArray(bodyBytes)).build()
        upperMethod == "POST" -> builder.POST(BodyPublishers.noBody()).build()
        else -> builder.method(upperMethod, BodyPublishers.noBody()).build()
    }
}

@PublishedApi
internal fun HttpProtocolVersion?.toJavaVersion(): JdkHttpClient.Version = when (this) {
    HttpProtocolVersion.HTTP_2_0 -> JdkHttpClient.Version.HTTP_2
    HttpProtocolVersion.HTTP_1_0 -> JdkHttpClient.Version.HTTP_1_1
    else -> JdkHttpClient.Version.HTTP_1_1
}

@PublishedApi
internal class KtorHttpResponseAdapter<T>(
    private val request: HttpRequest,
    private val uri: URI,
    private val version: JdkHttpClient.Version,
    private val statusCode: Int,
    private val headers: java.net.http.HttpHeaders,
    private val body: T
) : HttpResponse<T> {
    override fun statusCode(): Int = statusCode
    override fun request(): HttpRequest = request
    override fun previousResponse(): Optional<HttpResponse<T>> = Optional.empty()
    override fun headers(): java.net.http.HttpHeaders = headers
    override fun body(): T = body
    override fun sslSession(): Optional<SSLSession> = Optional.empty()
    override fun uri(): URI = uri
    override fun version(): JdkHttpClient.Version = version
}

suspend fun downloadFileWithProgress(
    url: String,
    targetPath: Path,
    onProgress: (bytesDownloaded: Long, totalBytes: Long, speed: Double) -> Unit
): Boolean = withContext(Dispatchers.IO) {
    try {
        val response = ktorHttpClient.get(url) {
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
