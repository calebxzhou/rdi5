package calebxzhou.rdi.service

import calebxzhou.mykotutils.std.sha1
import calebxzhou.mykotutils.std.toFixed
import calebxzhou.rdi.RDI
import calebxzhou.rdi.exception.ModpackException
import calebxzhou.rdi.model.pack.Mod
import calebxzhou.rdi.net.server
import calebxzhou.rdi.ui2.pointerBuffer
import org.bson.types.ObjectId
import org.lwjgl.util.tinyfd.TinyFileDialogs
import java.io.File
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
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
    val DL_MODS_DIR = RDI.DIR.resolve("dl-mods").apply { mkdirs() }
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
    suspend fun installVersion(modpackId: ObjectId, verName: String, mods: List<Mod>, onProgress: (String) -> Unit){
        val targetDir = GameService.versionListDir.resolve("${modpackId}_${verName}")
        val mods = CurseForgeService.downloadMods(mods,onProgress)
        val clientPack = downloadVersionClientPack(modpackId,verName,onProgress)
        if(clientPack==null){
            onProgress("客户端包下载失败，安装终止")
            return
        }
        if(targetDir.exists()){
            targetDir.deleteRecursively()
        }
        targetDir.mkdirs()
        onProgress("解压客户端整合包...")
        open(clientPack).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val relativePath = entry.name.trimStart('/')
                val destination = targetDir.resolve(relativePath)
                if (entry.isDirectory) {
                    destination.mkdirs()
                } else {
                    destination.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        destination.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }
        onProgress("建立mods软链接...")
        val modsDir = targetDir.resolve("mods").apply { mkdirs() }
        mods.forEach { modPath ->
            val modFile = modPath.toFile()
            val storedMod = DL_MODS_DIR.resolve(modFile.name)
            if(!storedMod.exists()){
                Files.copy(modFile.toPath(), storedMod.toPath())
            }
            val link = modsDir.resolve(modFile.name)
            if(link.exists()){
                link.delete()
            }
            try {
                Files.createSymbolicLink(link.toPath(), storedMod.toPath())
            } catch (ex: Exception){
                Files.copy(storedMod.toPath(), link.toPath())
            }
        }
        onProgress("整合包安装完成 位于:${targetDir.absolutePath}")
    }
    suspend fun downloadVersionClientPack(modpackId: ObjectId,verName: String,onProgress: (String) -> Unit): File? {
        val file = RDI.DIR.resolve("${modpackId}_$verName.zip")
        val hash = server.makeRequest<String>("modpack/$modpackId/version/$verName/client/hash").data
        if(file.exists() && file.sha1 == hash){
            return file
        }
        onProgress("下载客户端整合包...")
        server.download("modpack/$modpackId/version/$verName/client", file.toPath()){
            onProgress(it.percent.toFixed(2))
        }

        if(hash != file.sha1){
            onProgress("文件损坏了，请重新下载")
            file.delete()
            return null
        }
        return file
    }
}