package calebxzhou.rdi.service

import calebxzhou.rdi.exception.ModpackException
import calebxzhou.rdi.ui2.pointerBuffer
import org.lwjgl.util.tinyfd.TinyFileDialogs
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile

val selectModpackFile
    get() = TinyFileDialogs.tinyfd_openFileDialog(
    "选择整合包 (ZIP)",
    "C:/Users/${System.getProperty("user.name")}/Downloads",
    ("*.zip").pointerBuffer,
    "CurseForge整合包 (*.zip)",
    false
)
object ModpackService {
    fun open(zipFile: File): ZipFile {
        var lastError: Exception? = null
        val attempted = mutableListOf<String>()

        fun tryOpen(charset: Charset?): ZipFile? {
            return try {
                if (charset == null) ZipFile(zipFile) else ZipFile(zipFile, charset)
            } catch (ex: Exception) {
                lastError = ex
                attempted += charset?.name() ?: "system-default"
                null
            }
        }

        tryOpen(null)?.let { return it }
        buildList {
            add(StandardCharsets.UTF_8)
            add(Charset.defaultCharset())
            runCatching { add(Charset.forName("GB18030")) }.getOrNull()
            runCatching { add(Charset.forName("GBK")) }.getOrNull()
            add(StandardCharsets.ISO_8859_1)
        }.filterNotNull().distinct().forEach { charset ->
            tryOpen(charset)?.let { return it }
        }

        throw ModpackException(
            "无法读取整合包: ${lastError?.message ?: "未知错误"} (尝试编码: ${attempted.joinToString()})"
        )
    }
}