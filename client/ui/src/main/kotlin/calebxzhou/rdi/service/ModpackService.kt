package calebxzhou.rdi.service

import calebxzhou.mykotutils.curseforge.CFDownloadMod
import calebxzhou.mykotutils.curseforge.CurseForgeApi
import calebxzhou.mykotutils.std.openChineseZip
import calebxzhou.mykotutils.std.sha1
import calebxzhou.mykotutils.std.toFixed
import calebxzhou.rdi.RDI
import calebxzhou.rdi.model.pack.Mod
import calebxzhou.rdi.net.server
import calebxzhou.rdi.ui2.pointerBuffer
import org.bson.types.ObjectId
import org.lwjgl.util.tinyfd.TinyFileDialogs
import java.io.File
import java.nio.file.Files
import kotlin.io.path.name

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
    suspend fun installVersion(modpackId: ObjectId, verName: String, mods: List<Mod>, onProgress: (String) -> Unit){
        val versionDir = GameService.versionListDir.resolve("${modpackId}_${verName}")
        val mods = CurseForgeApi.downloadMods(
            mods.map {
                CFDownloadMod(it.projectId.toInt(), it.fileId.toInt(),it.slug, DL_MODS_DIR.resolve(it.fileName).toPath())
            }
        ){ cfmod,prog->
            onProgress("mod下载中：${cfmod.slug} ${prog.percent.toFixed(2)}")
        }.getOrThrow()
        val clientPack = downloadVersionClientPack(modpackId,verName,onProgress)
        if(clientPack==null){
            onProgress("客户端包下载失败，安装终止")
            return
        }
        if(versionDir.exists()){
            versionDir.deleteRecursively()
        }
        versionDir.mkdirs()
        onProgress("解压客户端整合包...")
        clientPack.openChineseZip().use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val relativePath = entry.name.trimStart('/')
                val destination = versionDir.resolve(relativePath)
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
        val modsDir = versionDir.resolve("mods").apply { mkdirs() }

        mods.forEach { dlmod ->
            Files.createSymbolicLink(modsDir.resolve(dlmod.path.fileName.name).toPath(), dlmod.path)
        }
        onProgress("整合包安装完成 位于:${versionDir.absolutePath}")
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