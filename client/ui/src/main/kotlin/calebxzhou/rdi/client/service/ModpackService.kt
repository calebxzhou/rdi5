package calebxzhou.rdi.client.service

import calebxzhou.mykotutils.std.humanFileSize
import calebxzhou.mykotutils.std.humanSpeed
import calebxzhou.mykotutils.std.openChineseZip
import calebxzhou.mykotutils.std.sha1
import calebxzhou.mykotutils.std.toFixed
import calebxzhou.mykotutils.std.urlEncoded
import calebxzhou.rdi.RDIClient
import calebxzhou.rdi.CONF
import calebxzhou.rdi.client.model.firstLoaderDir
import calebxzhou.rdi.client.net.server
import calebxzhou.rdi.client.ui2.McPlayArgs
import calebxzhou.rdi.common.DL_MOD_DIR
import calebxzhou.rdi.common.service.ModService
import calebxzhou.rdi.common.exception.RequestError
import calebxzhou.rdi.common.model.*
import calebxzhou.rdi.common.serdesJson
import calebxzhou.rdi.common.service.CurseForgeService
import calebxzhou.rdi.common.service.CurseForgeService.mapMods
import calebxzhou.rdi.common.service.ModrinthService
import calebxzhou.rdi.common.util.str
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.utils.io.streams.asInput
import kotlinx.coroutines.*
import kotlinx.io.buffered
import org.bson.types.ObjectId
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
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
        var clientPackFile: File? = null
        val versionDir = getVersionDir(modpackId, verName)

        val downloadModsTask = ModService.downloadModsTask(mods)

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

            val modFiles = mods.map { mod ->
                val file = DL_MOD_DIR.resolve(mod.fileName)
                if (!file.exists()) {
                    throw IllegalStateException("缺少Mod文件: ${file.absolutePath}")
                }
                file.toPath()
            }
            modFiles.forEachIndexed { index, modPath ->
                linkOrFail(modPath, modsDir.resolve(modPath.fileName.name).toPath())
                val fraction = (index + 1).toFloat() / modFiles.size.coerceAtLeast(1)
                ctx.emitProgress(TaskProgress("已链接 ${index + 1}/${modFiles.size}", fraction))
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

        val bgp = CONF.carrier != 0
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
    fun readLocalModpack(modpackFile: File){
        if(modpackFile.isDirectory){

        }else{
            runCatching {
                val zip = modpackFile.openChineseZip()

                zip
            }
        }
    }

    data class UploadPayload(
        val file: File,
        val mods: List<Mod>,
        val mcVersion: McVersion,
        val modloader: ModLoader,
        val sourceName: String,
        val sourceVersion: String
    )

    data class ParsedUploadPayload(
        val payload: UploadPayload,
        val mods: List<Mod>
    )

    suspend fun parseUploadPayload(
        file: File,
        onProgress: (String) -> Unit,
        onError: (String) -> Unit
    ): ParsedUploadPayload? {
        val packType = detectPackType(file)
        if (packType == PackType.MODRINTH) {
            val loaded = try {
                ModrinthService.loadModpack(file).getOrThrow()
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "解析整合包失败")
                return null
            }
            val mcVersion = loaded.mcVersion
            val modloader = loaded.modloader
            val versionName = loaded.index.versionId.ifBlank { "1.0" }
            val mods = loaded.mods
            return ParsedUploadPayload(
                UploadPayload(
                    file = loaded.file,
                    mods = mods,
                    mcVersion = mcVersion,
                    modloader = modloader,
                    sourceName = loaded.index.name,
                    sourceVersion = versionName
                ),
                mods
            )
        }

        if (packType != PackType.CURSEFORGE) {
            onError("无效的整合包文件：缺少 manifest.json 或 modrinth.index.json")
            onProgress("无效的整合包文件：缺少 manifest.json 或 modrinth.index.json")
            return null
        }
        val modpackData = try {
            CurseForgeService.loadModpack(file.absolutePath)
        } catch (e: Exception) {
            onError(e.message ?: "解析整合包失败")
            onProgress(e.message ?: "解析整合包失败")
            return null
        }

        return try {
            val mods = modpackData.manifest.files.mapMods()
            val mcVersion = McVersion.from(modpackData.manifest.minecraft.version)
            if (mcVersion == null) {
                onError("不支持的MC版本: ${modpackData.manifest.minecraft.version}")
                onProgress("不支持的MC版本: ${modpackData.manifest.minecraft.version}")
                return null
            }
            val modloader = ModLoader.from(modpackData.manifest.minecraft.modLoaders.firstOrNull()?.id.orEmpty())
            if (modloader == null) {
                onError("不支持的Mod加载器: ${modpackData.manifest.minecraft.modLoaders.firstOrNull()?.id.orEmpty()}")
                onProgress("不支持的Mod加载器: ${modpackData.manifest.minecraft.modLoaders.firstOrNull()?.id.orEmpty()}")
                return null
            }
            ParsedUploadPayload(
                UploadPayload(
                    file = modpackData.file,
                    mods = mods,
                    mcVersion = mcVersion,
                    modloader = modloader,
                    sourceName = modpackData.manifest.name,
                    sourceVersion = modpackData.manifest.version.ifBlank { "1.0" }
                ),
                mods
            )
        } catch (e: Exception) {
            onError("解析整合包失败: ${e.message}")
            onProgress("解析整合包失败: ${e.message}")
            null
        }
    }

    suspend fun uploadModpack(
        payload: UploadPayload,
        mods: List<Mod>,
        modpackName: String,
        versionName: String,
        updateModpackId: ObjectId?,
        onProgress: (String) -> Unit,
        onError: (String) -> Unit,
        onDone: (String) -> Unit
    ) {
        val modpack = getOrCreateModpack(payload, modpackName, updateModpackId, onProgress, onError) ?: return

        if (modpack.versions.any { it.name.equals(versionName, ignoreCase = true) }) {
            onError("${modpack.name}包已经有$versionName 这个版本了")
            return
        }

        val modpackId = modpack._id.toHexString()
        val versionEncoded = versionName.urlEncoded

        onProgress("创建版本 ${versionName}...")

        val totalBytes = payload.file.length()
        val startTime = System.nanoTime()
        var lastProgressUpdate = 0L
        val modsJson = serdesJson.encodeToString(mods)
        val boundary = "rdi-modpack-${System.currentTimeMillis()}"
        val multipartContent = MultiPartFormDataContent(
            formData {
                append(
                    key = "mods",
                    value = modsJson,
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    }
                )
                append(
                    key = "file",
                    value = InputProvider { payload.file.inputStream().asInput().buffered() },
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, ContentType.Application.Zip.toString())
                        append(HttpHeaders.ContentDisposition, "filename=\"${payload.file.name}\"")
                    }
                )
            },
            boundary = boundary
        )

        val createVersionResp = server.makeRequest<Unit>(
            path = "modpack/$modpackId/version/$versionEncoded",
            method = HttpMethod.Post,
        ) {
            timeout {
                requestTimeoutMillis = 60 * 60 * 1000L
                socketTimeoutMillis = 60 * 60 * 1000L
            }
            setBody(multipartContent)
            onUpload { bytesSentTotal, contentLength ->
                val now = System.nanoTime()
                val shouldUpdate = contentLength != null && bytesSentTotal == contentLength ||
                    now - lastProgressUpdate > 75_000_000L
                if (shouldUpdate) {
                    lastProgressUpdate = now
                    val elapsedSeconds = (now - startTime) / 1_000_000_000.0
                    val total = contentLength?.takeIf { it > 0 } ?: totalBytes
                    val percent = if (total <= 0) 100 else ((bytesSentTotal * 100) / total).toInt()
                    val speed = if (elapsedSeconds <= 0) 0.0 else bytesSentTotal / elapsedSeconds
                    onProgress(
                        buildString {
                            appendLine("正在上传版本 ${versionName}...")
                            appendLine(
                                "进度：${
                                    percent.coerceIn(
                                        0,
                                        100
                                    )
                                }% (${bytesSentTotal.humanFileSize}/${total.humanFileSize})"
                            )
                            appendLine("速度：${speed.humanSpeed}")
                        }
                    )
                }
            }
        }

        if (!createVersionResp.ok) {
            onError(createVersionResp.msg)
            return
        }

        val elapsedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0
        val speed = if (elapsedSeconds <= 0) 0.0 else totalBytes / elapsedSeconds
        onDone(
            buildString {
                appendLine("文件大小: ${totalBytes.humanFileSize}")
                appendLine("平均速度: ${speed.humanSpeed}")
                appendLine("耗时: ${"%.1f".format(elapsedSeconds)}秒")
                appendLine("传完了 服务器要开始构建 等5分钟 结果发你信箱里")
            }
        )
    }

    private suspend fun getOrCreateModpack(
        payload: UploadPayload,
        name: String,
        targetModpackId: ObjectId?,
        onProgress: (String) -> Unit,
        onError: (String) -> Unit
    ): Modpack? {
        onProgress(
            if (targetModpackId != null) {
                "获取整合包 ${name}..."
            } else {
                "检查整合包 ${name}..."
            }
        )
        val myModpacksResp = server.makeRequest<List<Modpack>>(
            path = "modpack/my",
            method = HttpMethod.Get
        )
        if (!myModpacksResp.ok) {
            onError(myModpacksResp.msg)
            return null
        }
        val myModpacks = myModpacksResp.data.orEmpty()

        if (targetModpackId != null) {
            val target = myModpacks.firstOrNull { it._id == targetModpackId }
            if (target == null) {
                onError("未找到要更新的整合包")
                return null
            }
            onProgress("为已有整合包 ${target.name} 上传新版")
            return target
        }

        val existing = myModpacks.firstOrNull { it.name.equals(name, ignoreCase = true) }
        if (existing != null) {
            onProgress("使用已有整合包 ${name}")
            return existing
        }

        try {
            validateModpackName(name)
        } catch (e: Exception) {
            onError(e.message ?: "整合包名称不合法")
            return null
        }
        onProgress("创建整合包 ${name}...")
        val mcVersion = payload.mcVersion
        val modloader = payload.modloader
        val createResp = server.makeRequest<Modpack>(
            path = "modpack",
            method = HttpMethod.Post,
            params = mapOf(
                "name" to name,
                "mcVer" to mcVersion,
                "modloader" to modloader
            )
        )
        if (!createResp.ok || createResp.data == null) {
            onError(createResp.msg)
            return null
        }
        return createResp.data
    }

    private enum class PackType { MODRINTH, CURSEFORGE, UNKNOWN }

    private fun detectPackType(file: File): PackType {
        return runCatching {
            file.openChineseZip().use { zip ->
                var hasMrIndex = false
                var hasCfManifest = false
                val entries = zip.entries().asSequence()
                for (entry in entries) {
                    if (entry.isDirectory) continue
                    when (entry.name.substringAfterLast('/')) {
                        "modrinth.index.json" -> hasMrIndex = true
                        "manifest.json" -> hasCfManifest = true
                    }
                    if (hasMrIndex || hasCfManifest) break
                }
                when {
                    hasMrIndex -> PackType.MODRINTH
                    hasCfManifest -> PackType.CURSEFORGE
                    else -> PackType.UNKNOWN
                }
            }
        }.getOrElse { PackType.UNKNOWN }
    }
}

