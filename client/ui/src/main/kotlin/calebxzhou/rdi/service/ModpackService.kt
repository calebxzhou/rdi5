package calebxzhou.rdi.service

import calebxzhou.mykotutils.curseforge.CFDownloadMod
import calebxzhou.mykotutils.curseforge.CFDownloadModException
import calebxzhou.mykotutils.curseforge.CurseForgeApi
import calebxzhou.mykotutils.std.humanFileSize
import calebxzhou.mykotutils.std.openChineseZip
import calebxzhou.mykotutils.std.sha1
import calebxzhou.mykotutils.std.toFixed
import calebxzhou.rdi.RDI
import calebxzhou.rdi.auth.LocalCredentials
import calebxzhou.rdi.common.DL_MOD_DIR
import calebxzhou.rdi.common.model.*
import calebxzhou.rdi.common.util.ioTask
import calebxzhou.rdi.common.util.str
import calebxzhou.rdi.model.McVersion
import calebxzhou.rdi.net.server
import calebxzhou.rdi.ui.component.alertErr
import calebxzhou.rdi.ui.component.confirm
import calebxzhou.rdi.ui.frag.TaskFragment
import calebxzhou.rdi.ui.go
import calebxzhou.rdi.ui.pointerBuffer
import calebxzhou.rdi.ui.toast
import kotlinx.coroutines.*
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
    val DL_PACKS_DIR = RDI.DIR.resolve("dl-packs").also { it.mkdirs() }
    fun getVersionDir(modpackId: ObjectId, verName: String): File {
        return GameService.versionListDir.resolve("${modpackId}_${verName}")
    }

    suspend fun installVersion(
        mcVersion: McVersion,
        modLoader: ModLoader,
        modpackId: ObjectId,
        verName: String,
        mods: List<Mod>,
        onProgress: (String) -> Unit
    ) {
        val versionDir = getVersionDir(modpackId, verName)
        val downloadedMods = CurseForgeApi.downloadMods(
            mods.map {
                CFDownloadMod(
                    it.projectId.toInt(),
                    it.fileId.toInt(),
                    it.slug,
                    DL_MOD_DIR.resolve(it.fileName).toPath()
                )
            }
        ) { cfmod, prog ->
            onProgress("mod下载中：${cfmod.slug} ${prog.percent.toFixed(2)}%")
        }.getOrElse {
            if (it is CFDownloadModException) {
                it.failed.forEach { (mod, ex) ->
                    onProgress("Mod ${mod} 下载失败:${ex.message}，安装终止")
                    ex.printStackTrace()
                }
            } else {
                it.printStackTrace()
                onProgress("未知错误:${it.message}，安装终止")

            }
            return
        }
        onProgress("所有Mod下载完成，准备下载客户端包...")
        val clientPack = downloadVersionClientPack(modpackId, verName, onProgress)
        if (clientPack == null) {
            onProgress("客户端包下载失败，安装终止")
            return
        }
        if (versionDir.exists()) {
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

        fun linkOrFail(src: java.nio.file.Path, dst: java.nio.file.Path) {
            runCatching {
                Files.deleteIfExists(dst)
                Files.createSymbolicLink(dst, src)
            }.onFailure {
                onProgress("创建符号链接失败，请以管理员权限重新运行：${dst.toFile().absolutePath}")
                throw IllegalStateException("符号链接创建失败，需要管理员权限")
            }
        }

        downloadedMods.forEach { dlmod ->
            linkOrFail(dlmod.path, modsDir.resolve(dlmod.path.fileName.name).toPath())
        }
        val mcSlug = "${mcVersion.mcVer}-${modLoader.name.lowercase()}"
        val mcCoreTarget = DL_MOD_DIR.resolve("rdi-5-mc-client-$mcSlug.jar").toPath()
        val mcCoreLink = modsDir.resolve("rdi-5-mc-client-$mcSlug.jar").toPath()
        if (mcCoreTarget.toFile().exists()) {
            withContext(Dispatchers.IO) {
                Files.deleteIfExists(mcCoreLink)
                linkOrFail(mcCoreTarget, mcCoreLink)
            }
        } else {
            onProgress("缺少核心文件: ${mcCoreTarget.toFile().absolutePath}")
            return
        }
        onProgress("写入语言文件...")
        """
            lang:zh_cn
            darkMojangStudiosBackground:true
            forceUnicodeFont:true
        """.trimIndent().let { versionDir.resolve("options.txt").writeText(it) }
        onProgress("整合包安装完成 位于:${versionDir.absolutePath}")
    }

    suspend fun downloadVersionClientPack(modpackId: ObjectId, verName: String, onProgress: (String) -> Unit): File? {
        val file = DL_PACKS_DIR.resolve("${modpackId}_$verName.zip")
        val hash = server.makeRequest<String>("modpack/$modpackId/version/$verName/client/hash").data
        if (file.exists() && file.sha1 == hash) {
            onProgress("客户端整合包已存在，跳过下载")
            return file
        }
        onProgress("下载客户端整合包...")
        server.download("modpack/$modpackId/version/$verName/client", file.toPath()) { prog ->
            val pct = if (prog.totalBytes > 0) {
                (prog.bytesDownloaded.toDouble() / prog.totalBytes * 100.0).toFixed(2)
            } else "--"
            onProgress(pct)
        }

        if (hash != file.sha1) {
            onProgress("文件损坏了，请重新下载")
            file.delete()
            return null
        }
        return file
    }

    fun Modpack.Version.startInstall(mcVer: String, modLoaderStr: String, modpackName: String? = null) {
        val mcVersion = McVersion.from(mcVer)
        val modLoader = runCatching { ModLoader.valueOf(modLoaderStr.uppercase()) }.getOrNull()
        if (mcVersion == null || modLoader == null) {
            alertErr("不支持的版本或加载器: $mcVer $modLoaderStr")
            return
        }

        TaskFragment("完整下载整合包 ${modpackName ?: ""} ${totalSize?.humanFileSize} ") {
            ModpackService.installVersion(mcVersion, modLoader, modpackId, this@startInstall.name, mods) { progress ->
                this.log(progress)
            }
        }.go()
    }

    fun isVersionInstalled(modpackId: ObjectId, verName: String): Boolean {
        val versionDir = getVersionDir(modpackId, verName)
        if (!versionDir.exists()) return false
        versionDir.resolve("mods").takeIf { it.exists() } ?: return false
        versionDir.resolve("config").takeIf { it.exists() } ?: return false
        return true
    }

    fun Host.startPlay() = ioTask {
        val status = server.makeRequest<HostStatus>("host/${_id}/status").run {
            data ?: run {
                alertErr("获取主机状态失败，无法游玩: ${this.msg}")
                return@ioTask
            }
        }
        val modpack = server.makeRequest<ModpackDetailedVo>("modpack/${modpackId}").run {
            data ?: run {
                alertErr("获取主机整合包信息失败，无法游玩: ${this.msg}")
                return@ioTask
            }
        }
        val version = server.makeRequest<Modpack.Version>("modpack/$modpackId/version/$packVer").run {
            data ?: run {
                alertErr("获取整合包版本信息失败，无法游玩: ${this.msg}")
                return@ioTask
            }
        }
        if (!isVersionInstalled(modpackId, packVer)) {
            confirm("未下载此主机的整合包，无法游玩。要现在开始下载吗？") {
                ioTask {
                    version.startInstall(modpack.mcVer, modpack.modloader)
                }
            }
            return@ioTask
        }
        if (status != HostStatus.PLAYABLE) {
            when (status) {
                HostStatus.STARTED -> {
                    toast("主机正在载入中 请稍等1~2分钟")
                }

                HostStatus.STOPPED -> {
                    server.requestU("host/${_id}/start") {
                        toast("主机已经启动 请稍等1~2分钟 可以在“后台”查看启动状态")
                    }
                }

                else -> {
                    alertErr("主机状态未知，无法游玩")
                    return@ioTask
                }
            }

        }
        if (!GameService.started) {
            val bgp = LocalCredentials.read().carrier != 0
            McVersion.from(modpack.mcVer)?.let { mcVersion ->
                TaskFragment("启动mc中") {
                    GameService.start(
                        mcVersion, "${modpackId.str}_${version.name}",
                        "-Drdi.ihq.url=${server.hqUrl}",
                        "-Drdi.game.ip=${if (bgp) server.bgpIp else server.ip}:${server.gamePort}",
                        "-Drdi.host.name=${this@startPlay.name}",
                        "-Drdi.host.port=${this@startPlay.port}"
                    ) {
                        this.log(it)
                    }
                }.go()
            } ?: alertErr("不支持的MC版本:${modpack.mcVer}  无法游玩")
        } else{
            alertErr("mc已在运行中，如需切换主机，请先关闭mc")
        }
    }

    data class LocalDir(
        val dir: File,
        val modpackId: ObjectId,
        val modpackName: String,
        val verName: String
    )
    suspend fun getLocalPackDirs(): List<LocalDir> = coroutineScope {
        val pattern = Regex("^([0-9a-fA-F]{24})_(.+)$")
        val dirs = GameService.versionListDir.listFiles()?.asSequence()
            ?.filter { it.isDirectory }
            ?.toList()
            ?: return@coroutineScope emptyList()

        val deferred = dirs.mapNotNull { dir ->
            val match = pattern.matchEntire(dir.name) ?: return@mapNotNull null
            val (idStr, verName) = match.destructured
            val packId = runCatching { ObjectId(idStr) }.getOrNull() ?: return@mapNotNull null
            async {
                val packName = server.makeRequest<String>("modpack/${idStr}/name").data ?: "未知整合包"
                LocalDir(dir, packId, packName, verName)
            }
        }

        deferred.awaitAll()
    }
}
