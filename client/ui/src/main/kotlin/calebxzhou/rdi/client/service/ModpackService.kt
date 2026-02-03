package calebxzhou.rdi.client.service

import calebxzhou.mykotutils.std.*
import calebxzhou.rdi.CONF
import calebxzhou.rdi.RDIClient
import calebxzhou.rdi.client.model.firstLoaderDir
import calebxzhou.rdi.client.net.server
import calebxzhou.rdi.client.ui2.McPlayArgs
import calebxzhou.rdi.common.DL_MOD_DIR
import calebxzhou.rdi.common.exception.ModpackException
import calebxzhou.rdi.common.exception.RequestError
import calebxzhou.rdi.common.model.*
import calebxzhou.rdi.common.serdesJson
import calebxzhou.rdi.common.service.CurseForgeService.loadInfoCurseForge
import calebxzhou.rdi.common.service.CurseForgeService.mapMods
import calebxzhou.rdi.common.service.ModService
import calebxzhou.rdi.common.service.ModrinthService
import calebxzhou.rdi.common.service.ModrinthService.mapModrinthVersions
import calebxzhou.rdi.common.service.ModrinthService.toCardVo
import calebxzhou.rdi.common.util.str
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.utils.io.streams.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.io.buffered
import org.bson.types.ObjectId
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.io.path.name


object ModpackService {
    val DL_PACKS_DIR = RDIClient.DIR.resolve("dl-packs").also { it.mkdirs() }
    val PACK_PROC_DIR = RDIClient.DIR.resolve("pack-proc").also { it.mkdirs() }
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
        data class NeedMc(val ver: McVersion) : StartPlayResult()
        data class NeedInstall(val task: Task) : StartPlayResult()
    }

    suspend fun Host.DetailVo.startPlay(): StartPlayResult {
        val statusResp = server.makeRequest<HostStatus>("host/${_id}/status")
        val status = statusResp.data ?: throw RequestError("获取地图状态失败: ${statusResp.msg}")

        val versionResp = server.makeRequest<Modpack.Version>("modpack/${modpack.id}/version/$packVer")
        val version = versionResp.data ?: throw RequestError("获取整合包版本信息失败: ${versionResp.msg}")

        if (!(modpack.mcVer.firstLoaderDir.exists())) {
            return StartPlayResult.NeedMc(modpack.mcVer)
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
        val sourceDir: File,
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
        val prepared = try {
            prepareModpackSource(file)
        } catch (e: Exception) {
            onError(e.message ?: "处理整合包失败")
            return null
        }
        val packType = detectPackType(prepared.rootDir)
        val embeddedMods = collectEmbeddedModFiles(prepared.rootDir)
        val embeddedMatches = matchEmbeddedModsAll(
            files = embeddedMods,
            onProgress = onProgress
        )
        if (embeddedMatches.removeFiles.isNotEmpty()) {
            embeddedMatches.removeFiles.forEach { it.delete() }
        }
        if (packType == PackType.MODRINTH) {
            val loaded = try {
                ModrinthService.loadModpack(prepared.rootDir).getOrThrow()
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e.message ?: "解析整合包失败")
                prepared.rootDir.deleteRecursively()
                return null
            }
            val mcVersion = loaded.mcVersion
            val modloader = loaded.modloader
            val versionName = loaded.index.versionId.ifBlank { "1.0" }
            val mods = (loaded.mods + embeddedMatches.mods).distinctBy { "${it.platform}:${it.projectId}:${it.fileId}:${it.hash}" }
            return ParsedUploadPayload(
                UploadPayload(
                    sourceDir = prepared.rootDir,
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
            prepared.rootDir.deleteRecursively()
            return null
        }
            val modpackData = try {
                loadCurseForgeFromDir(prepared.rootDir)
            } catch (e: Exception) {
                onError(e.message ?: "解析整合包失败")
                onProgress(e.message ?: "解析整合包失败")
                prepared.rootDir.deleteRecursively()
                return null
            }

            return try {
                val baseMods = modpackData.manifest.files.mapMods()
                val mods = (baseMods + embeddedMatches.mods).distinctBy { "${it.platform}:${it.projectId}:${it.fileId}:${it.hash}" }
                val mcVersion = McVersion.from(modpackData.manifest.minecraft.version)
                if (mcVersion == null) {
                    onError("不支持的MC版本: ${modpackData.manifest.minecraft.version}")
                    onProgress("不支持的MC版本: ${modpackData.manifest.minecraft.version}")
                    prepared.rootDir.deleteRecursively()
                    return null
                }
                val modloader = ModLoader.from(modpackData.manifest.minecraft.modLoaders.firstOrNull()?.id.orEmpty())
                if (modloader == null) {
                    onError("不支持的Mod加载器: ${modpackData.manifest.minecraft.modLoaders.firstOrNull()?.id.orEmpty()}")
                    onProgress("不支持的Mod加载器: ${modpackData.manifest.minecraft.modLoaders.firstOrNull()?.id.orEmpty()}")
                    prepared.rootDir.deleteRecursively()
                    return null
                }
                ParsedUploadPayload(
                    UploadPayload(
                        sourceDir = prepared.rootDir,
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

    private data class EmbeddedMatchResult(
        val mods: List<Mod>,
        val removeFiles: Set<File>
    )

    private data class EmbeddedMergedResult(
        val mods: List<Mod>,
        val removeFiles: Set<File>
    )

    private suspend fun matchEmbeddedModsCF(
        files: List<File>
    ): EmbeddedMatchResult {
        if (files.isEmpty()) return EmbeddedMatchResult(emptyList(), emptySet())
        val result = files.loadInfoCurseForge()
        val matched = result.matched
        if (matched.isEmpty()) return EmbeddedMatchResult(emptyList(), emptySet())
        val removeFiles = matched.mapNotNull { it.file }.toSet()
        result.matched.forEach { it.file = null }
        return EmbeddedMatchResult(matched, removeFiles)
    }

    private suspend fun matchEmbeddedModsMR(
        files: List<File>
    ): EmbeddedMatchResult {
        if (files.isEmpty()) return EmbeddedMatchResult(emptyList(), emptySet())
        val hashToVersion = files.mapModrinthVersions()
        if (hashToVersion.isEmpty()) return EmbeddedMatchResult(emptyList(), emptySet())
        val projectIds = hashToVersion.values.map { it.projectId }.distinct()
        val projectMap = ModrinthService.getMultipleProjects(projectIds).associateBy { it.id }
        val matched = mutableListOf<Mod>()
        val removeFiles = mutableSetOf<File>()
        files.forEach { file ->
            val sha1 = file.sha1
            val version = hashToVersion[sha1] ?: return@forEach
            val project = projectMap[version.projectId]
            val slug = project?.slug?.takeIf { it.isNotBlank() }
                ?: file.nameWithoutExtension.ifBlank { version.projectId }
            val side = project?.run {
                if (serverSide == "unsupported") {
                    return@run Mod.Side.CLIENT
                }
                if (clientSide == "unsupported") {
                    return@run Mod.Side.SERVER
                }
                Mod.Side.BOTH
            } ?: Mod.Side.BOTH
            val fileInfo = version.files.firstOrNull { it.hashes["sha1"] == sha1 }
            val downloadUrls = fileInfo?.url?.let { listOf(it) } ?: emptyList()
            val mod = Mod(
                platform = "mr",
                projectId = version.projectId,
                slug = slug,
                fileId = version.id,
                hash = sha1,
                side = side,
                downloadUrls = downloadUrls
            ).apply {
                this.file = null
                this.vo = project?.toCardVo(file)?.copy(side = side)
            }
            matched += mod
            removeFiles += file
        }
        return EmbeddedMatchResult(matched, removeFiles)
    }

    private suspend fun matchEmbeddedModsAll(
        files: List<File>,
        onProgress: (String) -> Unit
    ): EmbeddedMergedResult {
        if (files.isEmpty()) return EmbeddedMergedResult(emptyList(), emptySet())
        onProgress("发现整合包内置mod: ${files.size} 个，先匹配Modrinth")
        val mrResult = runCatching { matchEmbeddedModsMR(files) }
            .getOrDefault(EmbeddedMatchResult(emptyList(), emptySet()))
        val remaining = files.filterNot { it in mrResult.removeFiles }
        onProgress("Modrinth匹配完成：${mrResult.mods.size} 个，开始匹配CurseForge")
        val cfResult = runCatching { matchEmbeddedModsCF(remaining) }
            .getOrDefault(EmbeddedMatchResult(emptyList(), emptySet()))
        onProgress("匹配完成：MR ${mrResult.mods.size} 个，CF ${cfResult.mods.size} 个，处理结果中，请等一分钟...")
        val mergedMods = (mrResult.mods + cfResult.mods)
            .distinctBy { "${it.platform}:${it.projectId}:${it.fileId}:${it.hash}" }
        val removeFiles = mrResult.removeFiles + cfResult.removeFiles
        return EmbeddedMergedResult(mergedMods, removeFiles)
    }

    private data class PreparedModpack(
        val rootDir: File,
        val name: String
    )

    private fun prepareModpackSource(input: File): PreparedModpack {
        val tempDir = Files.createTempDirectory(PACK_PROC_DIR.toPath(), "pack-").toFile()
        if (input.isDirectory) {
            input.copyRecursively(tempDir, overwrite = true)
            return PreparedModpack(tempDir, input.name)
        }
        input.openChineseZip().use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val name = entry.name.replace('\\', '/').trimStart('/')
                if (name.isBlank()) return@forEach
                val outFile = tempDir.resolve(name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { inputStream ->
                        outFile.outputStream().use { output -> inputStream.copyTo(output) }
                    }
                }
            }
        }
        return PreparedModpack(tempDir, input.nameWithoutExtension)
    }

    private fun detectPackType(rootDir: File): PackType {
        var hasMrIndex = false
        var hasCfManifest = false
        rootDir.walkTopDown().forEach { file ->
            if (!file.isFile) return@forEach
            when (file.name) {
                "modrinth.index.json" -> hasMrIndex = true
                "manifest.json" -> hasCfManifest = true
            }
            if (hasMrIndex || hasCfManifest) return@forEach
        }
        return when {
            hasMrIndex -> PackType.MODRINTH
            hasCfManifest -> PackType.CURSEFORGE
            else -> PackType.UNKNOWN
        }
    }

    private fun findFile(rootDir: File, fileName: String): File? {
        return rootDir.walkTopDown().firstOrNull { it.isFile && it.name == fileName }
    }

    private fun loadCurseForgeFromDir(rootDir: File): CurseForgeModpackData {
        val manifestFile = findFile(rootDir, "manifest.json")
            ?: throw ModpackException("整合包缺少文件：manifest.json")
        val manifestJson = manifestFile.readText(Charsets.UTF_8)
        val manifest = runCatching {
            serdesJson.decodeFromString<CurseForgePackManifest>(manifestJson)
        }.getOrElse {
            throw ModpackException("manifest.json 解析失败: ${it.message}")
        }
        return CurseForgeModpackData(
            manifest = manifest,
            file = rootDir
        )
    }

    private fun collectEmbeddedModFiles(rootDir: File): List<File> {
        return rootDir.walkTopDown()
            .filter { it.isFile && it.extension.equals("jar", ignoreCase = true) }
            .filter { it.invariantSeparatorsPath.contains("/mods/") }
            .toList()
    }

    private fun buildZipFromDir(rootDir: File, baseName: String): File {
        val safeName = baseName.ifBlank { "modpack" }
        val target = PACK_PROC_DIR.resolve("${safeName}_${System.currentTimeMillis()}.zip")
        val addedDirs = mutableSetOf<String>()
        ZipOutputStream(target.outputStream()).use { out ->
            rootDir.walkTopDown().forEach { file ->
                if (file == rootDir) return@forEach
                val relative = file.relativeTo(rootDir).invariantSeparatorsPath
                if (relative.isBlank()) return@forEach
                val relativeLower = relative.lowercase()
                if (disallowedClientPaths.any { relativeLower.startsWith(it) }) return@forEach
                if (relativeLower.startsWith("kubejs/probe/")) return@forEach
                if (relativeLower.contains("cache")) return@forEach
                if (relativeLower.contains("yes_steve_model") || relativeLower.contains("史蒂夫模型")) return@forEach
                if (relativeLower.endsWith(".ogg") && file.length() > OGG_MAX_SIZE_BYTES) return@forEach
                if (relativeLower.endsWith(".mca") && relativeLower.contains("/saves/")) return@forEach
                if (isQuestLangEntryDisallowed(relativeLower, file.isDirectory)) return@forEach

                val topLevel = relative.substringBefore('/', relative)

                if (file.isDirectory) {
                    addDirectoryEntry(relative, out, addedDirs)
                    return@forEach
                }

                if (topLevel == "resourcepacks") {
                    val bytes = readResourcepackFile(file, relativeLower) ?: return@forEach
                    ensureZipParents(relative, out, addedDirs)
                    val entry = ZipEntry(relative).apply { time = file.lastModified() }
                    out.putNextEntry(entry)
                    out.write(bytes)
                    out.closeEntry()
                    return@forEach
                }

                ensureZipParents(relative, out, addedDirs)
                val entry = ZipEntry(relative).apply { time = file.lastModified() }
                out.putNextEntry(entry)
                if (relativeLower.endsWith(".zip")) {
                    val processedZip = processNestedZip(file)
                    out.write(processedZip)
                } else if (relativeLower.endsWith(".png")) {
                    val bytes = file.readBytes()
                    val processed = compressPngIfNeeded(bytes)
                    out.write(processed)
                } else {
                    file.inputStream().use { input -> input.copyTo(out) }
                }
                out.closeEntry()
            }
        }
        return target
    }

    private fun processNestedZip(zipFile: File): ByteArray {
        return ByteArrayOutputStream().use { baos ->
            ZipOutputStream(baos).use { out ->
                val addedDirs = mutableSetOf<String>()
                zipFile.openChineseZip().use { zip ->
                    zip.entries().asSequence().forEach { entry ->
                        val relative = entry.name.replace('\\', '/').trimStart('/')
                        if (relative.isBlank()) return@forEach
                        val relativeLower = relative.lowercase()
                        if (disallowedClientPaths.any { relativeLower.startsWith(it) }) return@forEach
                        if (relativeLower.startsWith("kubejs/probe/")) return@forEach
                        if (relativeLower.contains("cache")) return@forEach
                        if (relativeLower.contains("yes_steve_model") || relativeLower.contains("史蒂夫模型")) return@forEach
                        if (relativeLower.endsWith(".ogg") && entry.size != -1L && entry.size > OGG_MAX_SIZE_BYTES) return@forEach
                        if (relativeLower.endsWith(".mca") && relativeLower.contains("/saves/")) return@forEach
                        if (isQuestLangEntryDisallowed(relativeLower, entry.isDirectory)) return@forEach

                        val topLevel = relative.substringBefore('/', relative)

                        if (entry.isDirectory) {
                            addDirectoryEntry(relative, out, addedDirs)
                            return@forEach
                        }

                        if (topLevel == "resourcepacks") {
                            val bytes = readResourcepackEntry(zip, entry, relativeLower) ?: return@forEach
                            ensureZipParents(relative, out, addedDirs)
                            val zipEntry = ZipEntry(relative).apply { time = entry.time }
                            out.putNextEntry(zipEntry)
                            out.write(bytes)
                            out.closeEntry()
                            return@forEach
                        }

                        ensureZipParents(relative, out, addedDirs)
                        val zipEntry = ZipEntry(relative).apply { time = entry.time }
                        out.putNextEntry(zipEntry)
                        if (relativeLower.endsWith(".png")) {
                            val bytes = zip.getInputStream(entry).use { it.readBytes() }
                            val processed = compressPngIfNeeded(bytes)
                            out.write(processed)
                        } else if (relativeLower.endsWith(".ogg")) {
                            val size = entry.size
                            if (size != -1L) {
                                if (size > OGG_MAX_SIZE_BYTES) return@forEach
                                zip.getInputStream(entry).use { input -> input.copyTo(out) }
                            } else {
                                val bytes = zip.getInputStream(entry).use { it.readBytes() }
                                if (bytes.size > OGG_MAX_SIZE_BYTES) return@forEach
                                out.write(bytes)
                            }
                        } else {
                            zip.getInputStream(entry).use { input -> input.copyTo(out) }
                        }
                        out.closeEntry()
                    }
                }
            }
            baos.toByteArray()
        }
    }

    private fun readResourcepackEntry(source: java.util.zip.ZipFile, entry: ZipEntry, relativeLower: String): ByteArray? {
        if (entry.size != -1L && entry.size > RESOURCEPACK_MAX_SIZE_BYTES) {
            return null
        }
        val rawBytes = source.getInputStream(entry).use { input ->
            when {
                entry.size == -1L -> input.readBytes()
                entry.size > Int.MAX_VALUE -> return null
                entry.size > RESOURCEPACK_MAX_SIZE_BYTES -> return null
                else -> input.readNBytes(entry.size.toInt())
            }
        }
        if (relativeLower.endsWith(".ogg") && rawBytes.size > OGG_MAX_SIZE_BYTES) return null
        val processed = if (relativeLower.endsWith(".png")) compressPngIfNeeded(rawBytes) else rawBytes
        if (processed.size > RESOURCEPACK_MAX_SIZE_BYTES) return null
        return processed
    }

    private val disallowedClientPaths = setOf("shaderpacks")
    private val allowedQuestLangFiles = setOf("en_us.snbt", "zh_cn.snbt")
    private const val QUEST_LANG_PREFIX = "config/ftbquests/quests/lang/"
    private const val RESOURCEPACK_MAX_SIZE_BYTES = 1024L * 1024
    private const val OGG_MAX_SIZE_BYTES = 128L * 1024
    private const val PNG_COMPRESSION_THRESHOLD_BYTES = 50 * 1024
    private const val PNG_COMPRESSION_JPEG_QUALITY = 0.5f

    private fun isQuestLangEntryDisallowed(relativeLower: String, isDirectory: Boolean): Boolean {
        if (!relativeLower.startsWith(QUEST_LANG_PREFIX)) return false
        val remainder = relativeLower.removePrefix(QUEST_LANG_PREFIX)
        if (remainder.isEmpty()) return false
        if (isDirectory) return true
        if (remainder.contains('/')) return true
        return remainder !in allowedQuestLangFiles
    }

    private fun readResourcepackFile(file: File, relativeLower: String): ByteArray? {
        if (file.length() > RESOURCEPACK_MAX_SIZE_BYTES) return null
        if (relativeLower.endsWith(".ogg") && file.length() > OGG_MAX_SIZE_BYTES) return null
        val rawBytes = file.inputStream().use { input ->
            when {
                file.length() > Int.MAX_VALUE -> return null
                else -> input.readBytes()
            }
        }
        val processed = if (relativeLower.endsWith(".png")) compressPngIfNeeded(rawBytes) else rawBytes
        if (processed.size > RESOURCEPACK_MAX_SIZE_BYTES) return null
        return processed
    }

    private fun addDirectoryEntry(
        rawPath: String,
        output: ZipOutputStream,
        addedDirs: MutableSet<String>
    ) {
        val sanitized = rawPath.trim('/').ifEmpty { return }
        ensureZipParents(sanitized, output, addedDirs)
        val dirEntry = "$sanitized/"
        if (addedDirs.add(dirEntry)) {
            output.putNextEntry(ZipEntry(dirEntry))
            output.closeEntry()
        }
    }

    private fun ensureZipParents(path: String, output: ZipOutputStream, addedDirs: MutableSet<String>) {
        val normalized = path.trim('/').ifEmpty { return }
        val parts = normalized.split('/')
        if (parts.size <= 1) return
        var current = ""
        for (i in 0 until parts.size - 1) {
            val part = parts[i]
            if (part.isEmpty()) continue
            current = if (current.isEmpty()) part else "$current/$part"
            val dirEntry = "$current/"
            if (addedDirs.add(dirEntry)) {
                output.putNextEntry(ZipEntry(dirEntry))
                output.closeEntry()
            }
        }
    }

    private fun compressPngIfNeeded(bytes: ByteArray): ByteArray {
        if (bytes.size <= PNG_COMPRESSION_THRESHOLD_BYTES) return bytes
        return runCatching {
            val original = ImageIO.read(ByteArrayInputStream(bytes)) ?: return bytes
            val scaled = if (original.height > 720) {
                val targetHeight = 720
                val scale = targetHeight.toDouble() / original.height.toDouble()
                val targetWidth = (original.width * scale).toInt().coerceAtLeast(1)
                val resized = BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB)
                val g = resized.createGraphics()
                g.color = Color.WHITE
                g.fillRect(0, 0, resized.width, resized.height)
                g.drawImage(original, 0, 0, targetWidth, targetHeight, null)
                g.dispose()
                resized
            } else {
                original
            }
            val rgbImage = if (scaled.type == BufferedImage.TYPE_INT_RGB) scaled else {
                val converted = BufferedImage(scaled.width, scaled.height, BufferedImage.TYPE_INT_RGB)
                val graphics = converted.createGraphics()
                graphics.color = Color.WHITE
                graphics.fillRect(0, 0, converted.width, converted.height)
                graphics.drawImage(scaled, 0, 0, null)
                graphics.dispose()
                converted
            }
            val writerIterator = ImageIO.getImageWritersByFormatName("jpg")
            if (!writerIterator.hasNext()) return bytes
            val writer = writerIterator.next()
            try {
                val params = writer.defaultWriteParam
                if (params.canWriteCompressed()) {
                    params.compressionMode = ImageWriteParam.MODE_EXPLICIT
                    params.compressionQuality = PNG_COMPRESSION_JPEG_QUALITY
                }
                ByteArrayOutputStream().use { baos ->
                    val imageOut = ImageIO.createImageOutputStream(baos) ?: return bytes
                    imageOut.use { outputStream ->
                        writer.output = outputStream
                        writer.write(null, IIOImage(rgbImage, null, null), params)
                    }
                    baos.toByteArray()
                }
            } finally {
                writer.dispose()
            }
        }.getOrElse { bytes }
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
        onProgress("正在解压此包...请等一两分钟")
        val uploadZip = runCatching { buildZipFromDir(payload.sourceDir, payload.sourceName) }
            .getOrElse {
                payload.sourceDir.deleteRecursively()
                onError("打包失败: ${it.message}")
                return
            }

        if (modpack.versions.any { it.name.equals(versionName, ignoreCase = true) }) {
            onError("${modpack.name}包已经有$versionName 这个版本了")
            uploadZip.delete()
            payload.sourceDir.deleteRecursively()
            return
        }

        val modpackId = modpack._id.toHexString()
        val versionEncoded = versionName.urlEncoded

        onProgress("创建版本 ${versionName}...")

        val totalBytes = uploadZip.length()
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
                    value = InputProvider { uploadZip.inputStream().asInput().buffered() },
                    headers = Headers.build {
                        append(HttpHeaders.ContentType, ContentType.Application.Zip.toString())
                        append(HttpHeaders.ContentDisposition, "filename=\"${uploadZip.name}\"")
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
            uploadZip.delete()
            payload.sourceDir.deleteRecursively()
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
        uploadZip.delete()
        payload.sourceDir.deleteRecursively()
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

}
