package calebxzhou.rdi.ui.component

import calebxzhou.rdi.RDI
import calebxzhou.rdi.common.net.httpRequest
import calebxzhou.rdi.common.util.ioTask
import calebxzhou.rdi.lgr
import calebxzhou.rdi.ui.iconDrawable
import calebxzhou.rdi.ui.uiThread
import icyllis.modernui.core.Context
import icyllis.modernui.graphics.drawable.ImageDrawable
import icyllis.modernui.widget.ImageView
import io.ktor.client.request.*
import io.ktor.client.statement.*
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

class HttpImageView(context: Context, val imgUrl: String) : ImageView(context) {
    companion object {
        private val loadingImg = iconDrawable("loading")
        private val failLoadImg = iconDrawable("question")
        private val cacheDir = File(File(RDI.DIR, "cache"), "img")
    }


    init {
        if (!cacheDir.exists()) cacheDir.mkdirs()
        setImageDrawable(loadingImg)
        loadOrFetch(imgUrl)
    }

    private fun loadOrFetch(url: String) {
        val cacheFile = getCacheFile(url)
        if (cacheFile.exists()) {
            uiThread {
                loadFromDisk(cacheFile)?.let { bitmap ->
                    uiThread { setImageDrawable(bitmap) }
                } ?: run {
                    ioTask {
                        fetchAndCache(url, cacheFile)
                    }
                }
            }

        } else {
            ioTask { fetchAndCache(url, cacheFile) }
        }
    }

    private fun getCacheFile(url: String): File {
        val key = md5(url)
        return File(cacheDir, "$key.png")
    }

    private fun md5(text: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(text.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun loadFromDisk(file: File): ImageDrawable? {
        return try {
            FileInputStream(file).use { fis ->
                ImageDrawable(fis)
            }
        } catch (e: Exception) {
            lgr.warn("Failed to read cached image: ${e.message}")
            null
        }
    }

    private suspend fun fetchAndCache(url: String, cacheFile: File) {
        try {
            val response = httpRequest {
                url(url)
                method = io.ktor.http.HttpMethod.Get
            }
            if (response.status.value in 200..299) {
                val bytes = response.bodyAsBytes()
                // Save to disk
                try {
                    FileOutputStream(cacheFile).use { it.write(bytes) }
                } catch (e: Exception) {
                    lgr.warn("Failed to write image cache: ${e.message}")
                }
                // Decode and display
                uiThread {
                    val bitmap = decodeBitmap(ByteArrayInputStream(bytes))
                    if (bitmap != null) {
                        setImageDrawable(bitmap)
                        // Force the view to maintain its intended size
                        requestLayout()
                    } else {
                        setImageDrawable(failLoadImg)
                    }
                }
            } else {
                lgr.warn("HTTP ${response.status} loading image: $url")
                uiThread { setImageDrawable(failLoadImg) }
            }
        } catch (e: Exception) {
            lgr.error("Failed to load image $url: ${e.message}")
            uiThread { setImageDrawable(failLoadImg) }
        }
    }

    private fun decodeBitmap(data: ByteArrayInputStream): ImageDrawable? {
        return try {
            ImageDrawable(data)
        } catch (e: Exception) {
            lgr.warn("Failed to decode image: ${e.message}")
            null
        }
    }


}