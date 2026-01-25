package calebxzhou.rdi.client.service

import calebxzhou.mykotutils.curseforge.CFDownloadMod
import calebxzhou.mykotutils.curseforge.CFDownloadModException
import calebxzhou.mykotutils.curseforge.CurseForgeApi
import calebxzhou.mykotutils.std.humanFileSize
import calebxzhou.mykotutils.std.openChineseZip
import calebxzhou.mykotutils.std.sha1
import calebxzhou.mykotutils.std.toFixed
import calebxzhou.rdi.RDIClient
import calebxzhou.rdi.client.auth.LocalCredentials
import calebxzhou.rdi.client.model.firstLoaderDir
import calebxzhou.rdi.client.net.server
import calebxzhou.rdi.client.ui2.McPlayArgs
import calebxzhou.rdi.common.DL_MOD_DIR
import calebxzhou.rdi.common.exception.RequestError
import calebxzhou.rdi.common.model.*
import calebxzhou.rdi.common.util.str
import io.ktor.http.*
import kotlinx.coroutines.*
import org.bson.types.ObjectId
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.name


object ModpackService {
    val DL_PACKS_DIR = RDIClient.DIR.resolve("dl-packs").also { it.mkdirs() }
    fun getVersionDir(modpackId: ObjectId, verName: String): File {
        return GameService.versionListDir.resolve("${modpackId}_${verName}")
    }

    suspend fun downloadVersionClientPackLegacy(modpackId: ObjectId, verName: String, onProgress: (String) -> Unit): File? {
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

    fun installVersion(
        mcVersion: McVersion,
        modLoader: ModLoader,
        modpackId: ObjectId,
        verName: String,
        mods: List<Mod>
    ): Task {
        val downloadedMods = Collections.synchronizedList(mutableListOf<CFDownloadMod>())
        var clientPackFile: File? = null
        val versionDir = getVersionDir(modpackId, verName)

        val downloadModsTask = Task.Group(
            name = "下载Mod",
            subTasks = mods.map { mod ->
                Task.Leaf("mod ${mod.slug}") { ctx ->
                    val target = CFDownloadMod(
                        mod.projectId.toInt(),
                        mod.fileId.toInt(),
                        mod.slug,
                        DL_MOD_DIR.resolve(mod.fileName).toPath()
                    )
                    val result = CurseForgeApi.downloadMods(listOf(target)) { cfmod, prog ->
                        val fraction = if (prog.fraction > 1f) prog.fraction / 100f else prog.fraction
                        ctx.emitProgress(TaskProgress("下载中 ${cfmod.slug} ${prog.fraction.toFixed(2)}%", fraction))
                    }
                    val downloaded = result.getOrElse { err ->
                        if (err is CFDownloadModException) {
                            err.failed.forEach { (_, ex) ->
                                ex.printStackTrace()
                            }
                            throw IllegalStateException("Mod下载失败: ${err.failed.toMap().keys.joinToString(", ") { it.slug }}")
                        } else {
                            err.printStackTrace()
                            throw IllegalStateException("Mod下载失败: ${err.message}")
                        }
                    }
                    downloadedMods += downloaded
                    ctx.emitProgress(TaskProgress("下载完成", 1f))
                }
            }
        )

        val downloadClientPackTask = Task.Leaf("下载客户端整合包") { ctx ->
            val file = DL_PACKS_DIR.resolve("${modpackId}_$verName.zip")
            val hash = server.makeRequest<String>("modpack/$modpackId/version/$verName/client/hash").data
                ?: throw IllegalStateException("客户端包哈希为空")
            if (file.exists() && file.sha1 == hash) {
                ctx.emitProgress(TaskProgress("客户端整合包已存在", 1f))
                clientPackFile = file
                return@Leaf
            }
            ctx.emitProgress(TaskProgress("开始下载...", 0f))
            server.download("modpack/$modpackId/version/$verName/client", file.toPath()) { prog ->
                val fraction = if (prog.totalBytes > 0) {
                    prog.bytesDownloaded.toFloat() / prog.totalBytes
                } else {
                    null
                }
                val msg = if (prog.totalBytes > 0) {
                    "${prog.bytesDownloaded.humanFileSize}/${prog.totalBytes.humanFileSize}"
                } else {
                    prog.bytesDownloaded.humanFileSize
                }
                ctx.emitProgress(TaskProgress(msg, fraction))
            }
            if (hash != file.sha1) {
                file.delete()
                throw IllegalStateException("客户端包下载损坏，请重试")
            }
            clientPackFile = file
            ctx.emitProgress(TaskProgress("下载完成", 1f))
        }

        val extractTask = Task.Leaf("解压客户端整合包") { ctx ->
            val clientPack = clientPackFile ?: throw IllegalStateException("客户端包未准备好")
            if (versionDir.exists()) {
                versionDir.deleteRecursively()
            }
            versionDir.mkdirs()
            ctx.emitProgress(TaskProgress("解压中...", null))
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
            ctx.emitProgress(TaskProgress("解压完成", 1f))
        }

        val linkModsTask = Task.Leaf("建立mods软链接") { ctx ->
            val modsDir = versionDir.resolve("mods").apply { mkdirs() }

            fun linkOrFail(src: Path, dst: Path) {
                runCatching {
                    Files.deleteIfExists(dst)
                    Files.createSymbolicLink(dst, src)
                }.onFailure {
                    throw IllegalStateException("创建符号链接失败，需要管理员权限: ${dst.toFile().absolutePath}")
                }
            }

            downloadedMods.forEachIndexed { index, dlmod ->
                linkOrFail(dlmod.path, modsDir.resolve(dlmod.path.fileName.name).toPath())
                val fraction = (index + 1).toFloat() / downloadedMods.size.coerceAtLeast(1)
                ctx.emitProgress(TaskProgress("已链接 ${index + 1}/${downloadedMods.size}", fraction))
            }

            val mcSlug = "${mcVersion.mcVer}-${modLoader.name.lowercase()}"
            val mcCoreTarget = DL_MOD_DIR.resolve("rdi-5-mc-client-$mcSlug.jar").toPath()
            val mcCoreLink = modsDir.resolve("rdi-5-mc-client-$mcSlug.jar").toPath()
            if (mcCoreTarget.toFile().exists()) {
                Files.deleteIfExists(mcCoreLink)
                linkOrFail(mcCoreTarget, mcCoreLink)
            } else {
                throw IllegalStateException("缺少核心文件: ${mcCoreTarget.toFile().absolutePath}")
            }
            ctx.emitProgress(TaskProgress("链接完成", 1f))
        }

        val writeOptionsTask = Task.Leaf("写入语言文件") { ctx ->
            """
            lang:zh_cn
            darkMojangStudiosBackground:true
            forceUnicodeFont:true
            """.trimIndent().let { versionDir.resolve("options.txt").writeText(it) }
            ctx.emitProgress(TaskProgress("写入完成", 1f))
        }

        return Task.Sequence(
            name = "安装整合包 $verName",
            subTasks = listOf(
                downloadModsTask,
                downloadClientPackTask,
                extractTask,
                linkModsTask,
                writeOptionsTask
            )
        )
    }

    fun Modpack.Version.startInstall(
        mcVersion: McVersion,
        modLoader: ModLoader,
        modpackName: String? = null
    ): Task {
        val title = buildString {
            append("完整下载整合包")
            if (!modpackName.isNullOrBlank()) append(" ").append(modpackName)
            totalSize?.humanFileSize?.let { append(" ").append(it) }
        }
        return Task.Sequence(
            name = title,
            subTasks = listOf(
                installVersion(mcVersion, modLoader, modpackId, this@startInstall.name, mods)
            )
        )
    }

    fun isVersionInstalled(modpackId: ObjectId, verName: String): Boolean {
        val versionDir = getVersionDir(modpackId, verName)
        if (!versionDir.exists()) return false
        versionDir.resolve("mods").takeIf { it.exists() } ?: return false
        versionDir.resolve("config").takeIf { it.exists() } ?: return false
        return true
    }

    sealed class StartPlayResult {
        data class Ready(val args: McPlayArgs) : StartPlayResult()
        data class NeedInstall(val task: Task) : StartPlayResult()
    }

    suspend fun Host.DetailVo.startPlay(): StartPlayResult {
        val statusResp = server.makeRequest<HostStatus>("host/${_id}/status")
        val status = statusResp.data ?: throw RequestError("获取地图状态失败: ${statusResp.msg}")

        val versionResp = server.makeRequest<Modpack.Version>("modpack/${modpack.id}/version/$packVer")
        val version = versionResp.data ?: throw RequestError("获取整合包版本信息失败: ${versionResp.msg}")

        if (!(modpack.mcVer.firstLoaderDir.exists())) {
            throw RequestError("未安装MC版本资源：${modpack.mcVer.mcVer}")
        }
        if (!isVersionInstalled(modpack.id, packVer)) {
            val task = version.startInstall(modpack.mcVer, modpack.modloader, modpack.name)
            return StartPlayResult.NeedInstall(task)
        }

        when (status) {
            HostStatus.PLAYABLE, HostStatus.STARTED -> Unit
            HostStatus.STOPPED -> {
                val startResp = server.makeRequest<Unit>("host/${_id}/start", HttpMethod.Post)
                if (!startResp.ok) {
                    throw RequestError("启动地图失败: ${startResp.msg}")
                }
            }
            else -> throw RequestError("地图状态未知，无法游玩")
        }

        if (GameService.started) {
            throw RequestError("mc运行中，如需切换要玩的地图，请先关闭mc")
        }

        val bgp = LocalCredentials.read().carrier != 0
        val versionId = "${modpack.id.str}_${version.name}"
        val args = listOf(
            "-Drdi.ihq.url=${server.hqUrl}",
            "-Drdi.game.ip=${if (bgp) server.bgpIp else server.ip}:${server.gamePort}",
            "-Drdi.host.name=${name}",
            "-Drdi.host.port=${port}"
        )
        return StartPlayResult.Ready(
            McPlayArgs(
            title = "游玩 $name",
            mcVer = modpack.mcVer,
            versionId = versionId,
            jvmArgs = args
            )
        )
    }

    data class LocalDir(
        val dir: File,
        val verName: String,
        val vo: Modpack.BriefVo
    ){
        val versionId = dir.name
    }
    suspend fun getLocalPackDirs(): List<LocalDir> = coroutineScope {
        val pattern = Regex("^([0-9a-fA-F]{24})_(.+)$")
        val dirs = GameService.versionListDir.listFiles()?.asSequence()
            ?.filter { it.isDirectory }
            ?.toList()
            ?: return@coroutineScope emptyList()

        val deferred = dirs.mapNotNull { dir ->
            val match = pattern.matchEntire(dir.name) ?: return@mapNotNull null
            val (idStr, verName) = match.destructured
            async {
                val vo = server.makeRequest<Modpack.BriefVo>("modpack/${idStr}/brief").data ?: Modpack.BriefVo()
                LocalDir(dir,verName, vo)
            }
        }

        deferred.awaitAll()
    }
}

