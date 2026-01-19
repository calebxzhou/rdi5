package calebxzhou.rdi.client.service

import calebxzhou.mykotutils.ktor.DownloadProgress
import calebxzhou.mykotutils.ktor.downloadFileFrom
import calebxzhou.mykotutils.log.Loggers
import calebxzhou.mykotutils.std.*
import calebxzhou.rdi.CONF
import calebxzhou.rdi.RDIClient
import calebxzhou.rdi.client.Const
import calebxzhou.rdi.client.model.*
import calebxzhou.rdi.client.net.loggedAccount
import calebxzhou.rdi.common.json
import calebxzhou.rdi.common.model.*
import calebxzhou.rdi.common.model.LibraryOsArch.Companion.detectHostOs
import calebxzhou.rdi.common.serdesJson
import calebxzhou.rdi.common.util.toUUID
import com.sun.management.OperatingSystemMXBean
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import java.lang.management.ManagementFactory
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.zip.ZipFile

object GameService {
    private val lgr by Loggers
    var started = false
        private set
    val DIR = File(RDIClient.DIR, "mc").apply { mkdirs() }
    private val libsDir = DIR.resolve("libraries").apply { mkdirs() }
    private val assetsDir = DIR.resolve("assets").apply { mkdirs() }
    private val assetIndexesDir = assetsDir.resolve("indexes").apply { mkdirs() }
    private val assetObjectsDir = assetsDir.resolve("objects").apply { mkdirs() }
    val versionListDir = DIR.resolve("versions").apply { mkdirs() }
    private val hostOs = detectHostOs()
    private val hostOsArchRaw = System.getProperty("os.arch")?.lowercase(Locale.ROOT) ?: ""
    private val hostOsVersionRaw = System.getProperty("os.version") ?: ""
    private val launcherFeatures: Map<String, Boolean> = emptyMap()
    private val locale = Locale.SIMPLIFIED_CHINESE
    private val MAX_PARALLEL_LIBRARY_DOWNLOADS = Runtime.getRuntime().availableProcessors()
    private val MAX_PARALLEL_ASSET_DOWNLOADS = Runtime.getRuntime().availableProcessors()
    private val mirrors = mapOf(
        "https://maven.neoforged.net/releases" to "https://bmclapi2.bangbang93.com/maven",
        "https://files.minecraftforge.net/maven" to "https://bmclapi2.bangbang93.com/maven",
        "http://launchermeta.mojang.com/mc/game/version_manifest.json" to "https://bmclapi2.bangbang93.com/mc/game/version_manifest.json",
        "http://launchermeta.mojang.com/mc/game/version_manifest_v2.json" to "https://bmclapi2.bangbang93.com/mc/game/version_manifest_v2.json",
        "https://launchermeta.mojang.com" to "https://bmclapi2.bangbang93.com",
        "https://launcher.mojang.com" to "https://bmclapi2.bangbang93.com",
        "http://resources.download.minecraft.net" to "https://bmclapi2.bangbang93.com/assets",
        "https://libraries.minecraft.net" to "https://bmclapi2.bangbang93.com/maven",
    )
    private val bracketedLibraryRegex = Regex("^\\[(.+)]$")
    private val numberedAssetRegex = Regex("^(.+?)(\\d+)(\\.[^./]+)$")
    internal val String.rewriteMirrorUrl: String
        get() {
            if (!CONF.useMirror) return this
            val original = this
            mirrors.forEach { (originRaw, mirrorRaw) ->
                val origin = originRaw.trim()
                if (origin.isEmpty()) return@forEach
                if (original.startsWith(origin)) {
                    val suffix = original.removePrefix(origin)
                    val mirror = mirrorRaw.trim()
                    if (suffix.isEmpty()) return mirror
                    val normalizedSuffix = suffix.trimStart('/')
                    val normalizedMirror = mirror.trimEnd('/')
                    return "$normalizedMirror/$normalizedSuffix"
                }
            }
            return original
        }


    suspend fun downloadVersionLegacy(version: McVersion, onProgress: (String) -> Unit) {
        onProgress("正在获取 ${version.mcVer} 的版本信息...")
        val manifest = version.metadata
        downloadClientLegacy(manifest, onProgress)
        downloadLibrariesLegacy(manifest, onProgress)
        extractNatives(manifest, onProgress)
        downloadAssetsLegacy(manifest, onProgress)
        onProgress("$version 所需文件下载完成")
    }

    fun downloadVersion(version: McVersion, loader: ModLoader? = null): Task {
        val manifest = version.metadata
        val tasks = mutableListOf<Task>(
            downloadClient(manifest),
            downloadLibraries(manifest.libraries),
            Task.Leaf("提取原生库") { ctx ->
                extractNatives(manifest) { message ->
                    ctx.emitProgress(TaskProgress(message, null))
                }
                ctx.emitProgress(TaskProgress("完成", 1f))
            },
            downloadAssets(manifest)
        )
        loader?.let { tasks += downloadLoader(version, it) }
        return Task.Sequence(
            name = "下载 $version",
            subTasks = tasks
        )
    }

    private fun extractNatives(manifest: MojangVersionManifest, onProgress: (String) -> Unit) {
        val nativesDir = versionListDir.resolve(manifest.id).resolve("natives").apply { mkdirs() }
        manifest.libraries.filterNativeOnly.forEach { library ->
            val jarFile = library.file
            if (!jarFile.exists()) {
                onProgress("运行库${library.name}下载失败，无法提取")
                return@forEach
            }
            onProgress("提取 ${library.name} 的原生库")
            ZipFile(jarFile).use { zip ->
                zip.entries().asSequence()
                    .filter { !it.isDirectory && it.name.isNativeLibraryName() }
                    .forEach { entry ->
                        val target = nativesDir.resolve(entry.name.substringAfterLast('/'))
                        target.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            target.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
            }
        }
    }

    private fun String.isNativeLibraryName(): Boolean {
        val lower = lowercase(Locale.ROOT)
        return lower.endsWith(".dll") || lower.endsWith(".so") || lower.endsWith(".dylib")
    }

    suspend fun downloadClientLegacy(manifest: MojangVersionManifest, onProgress: (String) -> Unit) {
        manifest.id
        var clientArtf = manifest.downloads?.client ?: return
        if (CONF.useMirror) {
            clientArtf = MojangDownloadArtifact(
                url = "https://bmclapi2.bangbang93.com/version/${manifest.id}/client",
                sha1 = clientArtf.sha1,
                size = clientArtf.size,
                path = clientArtf.path
            )
        }
        val versionDir = versionListDir.resolve(manifest.id).apply { mkdirs() }
        File(versionDir, "${manifest.id}.json").writeText(manifest.json)
        val target = File(versionDir, "${manifest.id}.jar")
        downloadArtifactLegacy("客户端核心 ${manifest.id}", clientArtf, target, onProgress)
    }

    fun downloadClient(manifest: MojangVersionManifest): Task {
        return Task.Leaf("下载客户端 ${manifest.id}") { ctx ->
            var clientArtf = manifest.downloads?.client ?: run {
                ctx.emitProgress(TaskProgress("缺少客户端下载信息", 0f))
                return@Leaf
            }
            if (CONF.useMirror) {
                clientArtf = MojangDownloadArtifact(
                    url = "https://bmclapi2.bangbang93.com/version/${manifest.id}/client",
                    sha1 = clientArtf.sha1,
                    size = clientArtf.size,
                    path = clientArtf.path
                )
            }
            val versionDir = versionListDir.resolve(manifest.id).apply { mkdirs() }
            File(versionDir, "${manifest.id}.json").writeText(manifest.json)
            val target = File(versionDir, "${manifest.id}.jar")
            ctx.emitProgress(TaskProgress("开始下载...", 0.1f))
            downloadArtifact("客户端核心 ${manifest.id}", clientArtf, target) { progress ->
                ctx.emitProgress(
                    TaskProgress(
                        "${progress.bytesDownloaded.humanFileSize}/${progress.totalBytes.humanFileSize}",
                        progress.fraction
                    )
                )
            }.getOrThrow()
            ctx.emitProgress(TaskProgress("下载完成", 1f))
        }
    }

    /*   private suspend fun loadBaseManifest(version: McVersion): MojangVersionManifest {
           val manifestFile = versionListDir.resolve(version.mcVer).resolve("${version.mcVer}.json")
           val localJson = runCatching { manifestFile.takeIf { it.exists() }?.readText() }.getOrNull()
           if (!localJson.isNullOrBlank()) {
               return serdesJson.decodeFromString(localJson)
           }
           return httpRequest { url(version.metaUrl) }.body()
       }*/

    suspend fun downloadLibrariesLegacy(manifest: MojangVersionManifest, onProgress: (String) -> Unit) {
        downloadLibrariesLegacy(manifest.libraries, onProgress)
    }

    private suspend fun downloadLibrariesLegacy(libraries: List<MojangLibrary>, onProgress: (String) -> Unit) =
        coroutineScope {
            val semaphore = Semaphore(MAX_PARALLEL_LIBRARY_DOWNLOADS)
            libraries
                .filter { it.shouldDownloadByArch() }
                .apply { onProgress("需要下载的库文件: ${this.size}个： ${this.joinToString("\n") { it.name }}") }
                .map { library ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            downloadLibraryArtifactsLegacy(library, onProgress)
                        }
                    }
                }.awaitAll()
        }


    private fun MojangLibrary.shouldDownloadByArch(): Boolean {
        //没有系统要求的直接下载
        if (rules == null) return true
        val allowOs = rules.filter { it.action == MojangRuleAction.allow }.mapNotNull { it.os?.name }
        return hostOs.ruleOsName in allowOs && this.name.endsWith(hostOs.archSuffix)
    }

    //只有native的lib
    private val List<MojangLibrary>.filterNativeOnly
        get() = this.filter { it.rules != null }
            .filter { it.shouldDownloadByArch() }
    val MojangLibrary.file get() = File(libsDir, this.downloads.artifact.path!!)
    private suspend fun downloadLibraryArtifactsLegacy(library: MojangLibrary, onProgress: (String) -> Unit): File {
        return downloadLibraryArtifact(library) { progress ->
            onProgress("${library.name} ${progress.bytesDownloaded.humanFileSize}/${progress.totalBytes.humanFileSize}")
        }.getOrThrow()
    }

    private suspend fun downloadLibraryArtifact(
        library: MojangLibrary,
        onProgress: (DownloadProgress) -> Unit
    ): Result<File> {
        val artifact = library.downloads.artifact
        val relativePath = artifact.path ?: return Result.failure(IllegalStateException("缺少库路径"))
        val target = File(libsDir, relativePath)
        if (target.exists()) {
            val existingSha = runCatching { target.sha1 }.getOrNull()
            if (existingSha != null && existingSha.equals(artifact.sha1, true)) {
                return Result.success(target)
            }
        }

        target.parentFile?.mkdirs()
        val maxRetries = 4
        var attempt = 0
        while (true) {
            val result = target.toPath().downloadFileFrom(
                url = if(attempt<2)artifact.url.rewriteMirrorUrl else artifact.url,
                knownSize = artifact.size
            ) { progress ->
                onProgress(progress)
            }
            val error = result.exceptionOrNull()
            if (error != null) {
                target.delete()
                if (attempt >= maxRetries || !isTooManyRequests(error)) {
                    throw error
                }
                val backoffMs = 1000L * (attempt + 1)
                delay(backoffMs)
                attempt++
                continue
            }
            val downloadedSha = target.sha1
            if (!downloadedSha.equals(artifact.sha1, true)) {
                target.delete()
                if (attempt >= maxRetries) {
                    throw IllegalStateException("${library.name} 校验失败")
                }
                val backoffMs = 1000L * (attempt + 1)
                delay(backoffMs)
                attempt++
                continue
            }
            return Result.success(target)
        }
    }

    private suspend fun downloadArtifactLegacy(
        label: String,
        artifact: MojangDownloadArtifact,
        target: File,
        onProgress: (String) -> Unit
    ) {
        if (target.exists()) {
            val existingSha = runCatching { target.sha1 }.getOrNull()
            if (existingSha != null && existingSha.equals(artifact.sha1, true)) {
                onProgress("跳过 $label (已存在)")
                return
            }
        }

        target.parentFile?.mkdirs()
        onProgress("下载 $label...")
        val success = target.toPath().downloadFileFrom(artifact.url.rewriteMirrorUrl) { progress ->
            val percent = progress.fraction.takeIf { it >= 0 }
                ?.let { String.format(locale, "%.1f%%", it) }
                ?: "--"
            onProgress("$label 下载中 $percent")
        }.getOrElse {
            target.delete()
            throw it
        }
        val downloadedSha = target.sha1
        if (!downloadedSha.equals(artifact.sha1, true)) {
            target.delete()
            throw IllegalStateException("$label 校验失败")
        }
    }

    suspend fun downloadAssetsLegacy(manifest: MojangVersionManifest, onProgress: (String) -> Unit) {
        val assetIndexMeta = manifest.assetIndex ?: let { onProgress("找不到资源"); return }
        val metaJson = this.jarResource("mcmeta/assets-index/${assetIndexMeta.id}.json").readAllString()
        val index = serdesJson.decodeFromString<MojangAssetIndexFile>(metaJson)
        assetIndexesDir.resolve("${assetIndexMeta.id}.json").writeText(metaJson)

        val toDownload = mutableListOf<Map.Entry<String, MojangAssetObject>>()
        val toStub = mutableListOf<Map.Entry<String, MojangAssetObject>>()

        index.objects.entries.forEach { entry ->
            when {
                shouldDownloadAsset(entry.key) -> toDownload += entry
                shouldUseEmptySound(entry.key) -> toStub += entry
                else -> Unit
            }
        }

        onProgress("准备下载资源 (${toDownload.size}/${index.objects.size}) ... 空音频替换 ${toStub.size} 个")

        // Write stub ogg files for skipped sounds
        toStub.forEach { (_, obj) ->
            writeEmptySoundStub(obj.hash)
        }

        val errors = Collections.synchronizedList(mutableListOf<String>())
        supervisorScope {
            val semaphore = Semaphore(MAX_PARALLEL_ASSET_DOWNLOADS)
            toDownload.map { (path, obj) ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        runCatching {
                            onProgress("开始下载资源 $path")
                            //downloadAssetObject(path, obj, onProgress)
                        }.onFailure { throwable ->
                            val message = throwable.message ?: throwable::class.simpleName ?: "unknown"
                            errors += "$path: $message"
                            onProgress("资源 $path 下载失败: $message")
                        }
                    }
                }
            }.awaitAll()
        }
        if (errors.isNotEmpty()) {
            val preview = errors.take(3).joinToString()
            throw IllegalStateException("共有 ${errors.size} 个资源下载失败，例如: $preview")
        }
    }


    fun downloadLibraries(libraries: List<MojangLibrary>): Task {
        val filtered = libraries.filter { it.shouldDownloadByArch() }
        if (filtered.isEmpty()) {
            return Task.Leaf("下载运行库") { ctx ->
                ctx.emitProgress(TaskProgress("无需下载", 1f))
            }
        }
        val subTasks = filtered.map { library ->
            Task.Leaf("运行库 ${library.name}") { ctx ->
                downloadSingleLibrary(library, ctx)
            }
        }
        return Task.Group(
            name = "下载${filtered.size}个运行库",
            subTasks = subTasks
        )
    }

    fun downloadAssets(manifest: MojangVersionManifest): Task {
        val assetIndexMeta = manifest.assetIndex ?: return Task.Leaf("下载资源") { ctx ->
            ctx.emitProgress(TaskProgress("找不到资源", 0f))
        }
        val metaJson = jarResource("mcmeta/assets-index/${assetIndexMeta.id}.json").readAllString()
        val index = serdesJson.decodeFromString<MojangAssetIndexFile>(metaJson)
        if(!assetIndexesDir.exists()){
            assetIndexesDir.mkdirs()
        }
        assetIndexesDir.resolve("${assetIndexMeta.id}.json").writeText(metaJson)

        val toDownload = mutableListOf<Map.Entry<String, MojangAssetObject>>()
        val toStub = mutableListOf<Map.Entry<String, MojangAssetObject>>()

        index.objects.entries.forEach { entry ->
            when {
                shouldDownloadAsset(entry.key) -> toDownload += entry

                shouldUseEmptySound(entry.key) -> toStub += entry
                else -> Unit
            }
        }

        toStub.forEach { (_, obj) -> writeEmptySoundStub(obj.hash) }

        val linkPlans = mutableListOf<AssetLinkPlan>()
        val grouped = mutableMapOf<String, MutableList<NumberedAsset>>()
        val normal = mutableListOf<Map.Entry<String, MojangAssetObject>>()

        toDownload.forEach { entry ->
            val path = entry.key
            val match = numberedAssetRegex.matchEntire(path)
            if (match == null) {
                normal += entry
            } else {
                val baseKey = match.groupValues[1] + match.groupValues[3]
                val number = match.groupValues[2].toIntOrNull()
                if (number == null) {
                    normal += entry
                } else {
                    grouped.getOrPut(baseKey) { mutableListOf() }
                        .add(NumberedAsset(path, entry.value, number))
                }
            }
        }

        val compacted = mutableListOf<Map.Entry<String, MojangAssetObject>>()
        compacted += normal
        grouped.values.forEach { group ->
            if (group.size <= 1) {
                val only = group.first()
                compacted += mapEntry(only.path, only.asset)
                return@forEach
            }
            val preferred = group.firstOrNull { it.number == 1 }
                ?: group.minBy { it.number }
            compacted += mapEntry(preferred.path, preferred.asset)
            group.filter { it != preferred }.forEach { other ->
                if (!other.asset.hash.equals(preferred.asset.hash, true)) {
                    linkPlans += AssetLinkPlan(other.asset.hash, preferred.asset.hash, other.path)
                }
            }
        }

        val subTasks = compacted.map { (path, obj) ->
            Task.Leaf("资源 $path") { ctx ->
                ctx.emitProgress(TaskProgress("开始下载...", 0f))
                downloadAssetObject(path, obj) { prog ->
                    ctx.emitProgress(TaskProgress("${prog.bytesDownloaded.humanFileSize}/${prog.totalBytes.humanFileSize}",prog.fraction))
                }.getOrThrow()
                ctx.emitProgress(TaskProgress("下载完成", 1f))
            }
        }

        if (linkPlans.isNotEmpty()) {
            //相似音效只用同一份
            val linksTask = Task.Leaf("链接相似资源") { ctx ->
                linkPlans.forEachIndexed { index, plan ->
                    createObjectLink(plan.fromHash, plan.toHash)
                    ctx.emitProgress(TaskProgress("已链接 ${index + 1}/${linkPlans.size}", (index + 1).toFloat() / linkPlans.size))
                }
            }
            return Task.Group(
                name = "下载${compacted.size}个音频资源",
                subTasks = subTasks + linksTask
            )
        }

        return Task.Group(
            name = "下载${compacted.size}个音频资源",
            subTasks = subTasks
        )
    }

    private suspend fun downloadAssetObject(
        path: String,
        asset: MojangAssetObject,
        onProgress: (DownloadProgress) -> Unit
    ): Result<File> {
        val hash = asset.hash.lowercase(Locale.ROOT)
        val targetDir = assetObjectsDir.resolve(hash.substring(0, 2))
        val targetFile = targetDir.resolve(hash)

        // Check existing file
        if (targetFile.exists()) {
            // Optimization: If file size matches, check SHA1.
            // Often checking size first is enough to skip broken downloads quickly without full hash calc
            if (targetFile.length() == asset.size) {
                // Optional: You can do full SHA1 check here if you want strict integrity
                // For speed, many launchers trust size matches during startup checks
                val existingSha = runCatching { targetFile.sha1 }.getOrNull()
                if (existingSha != null && existingSha.equals(hash, true)) {
                    // onProgress("跳过资源 $path (已存在)") // Reduce spam
                    return Result.success(targetFile)
                }
            }
        }

        targetDir.mkdirs()
        val downloadUrl = buildAssetUrl(hash)

        val maxRetries = 4
        var attempt = 0
        while (true) {
            val result = targetFile.toPath().downloadFileFrom(
                url = downloadUrl,
                knownSize = asset.size // <--- PASS THE SIZE HERE
            ) { progress ->
                onProgress(progress)
            }
            val error = result.exceptionOrNull() ?: break
            targetFile.delete()
            if (attempt >= maxRetries || !isTooManyRequests(error)) {
                throw error
            }
            //过一会重试
            val backoffMs = 1000L * (attempt + 1)
            delay(backoffMs)
            attempt++
        }

        if (targetFile.length() != asset.size) {
            targetFile.delete()
            throw IllegalStateException("Size mismatch for $path")
        }
        return Result.success(targetFile)

        // Optional: strict hash check after download (costly for CPU)
        // If speed is paramount, trust TCP/TLS checksums + file size for assets
        // val downloadedSha = targetFile.sha1 ...
    }

    private fun readInstallerEntry(installer: File, entryName: String): String {
        ZipFile(installer).use { zip ->
            val entry = zip.getEntry(entryName)
                ?: throw IllegalStateException("安装器中缺少 $entryName")
            zip.getInputStream(entry).bufferedReader(StandardCharsets.UTF_8).use { reader ->
                return reader.readText()
            }
        }
    }

    private suspend fun downloadMojmapIfNeededLegacy(
        installProfile: LoaderInstallProfile,
        vanillaManifest: MojangVersionManifest,
        onProgress: (String) -> Unit
    ) {
        val mojmaps = installProfile.data["MOJMAPS"] ?: return
        val downloads = vanillaManifest.downloads ?: return
        val tasks = mutableListOf<Triple<String, MojangDownloadArtifact, String>>()
        extractLibraryDescriptor(mojmaps.client)?.let { descriptor ->
            downloads.clientMappings?.let { artifact ->
                tasks += Triple("客户端", artifact, descriptor)
            }
        }
        extractLibraryDescriptor(mojmaps.server)?.let { descriptor ->
            downloads.serverMappings?.let { artifact ->
                tasks += Triple("服务端", artifact, descriptor)
            }
        }
        tasks.forEach { (label, artifact, descriptor) ->
            val relativePath = descriptorToLibraryPath(descriptor)
            val target = File(libsDir, relativePath)
            downloadArtifactLegacy("$label Mojmap", artifact, target, onProgress)
        }
    }

    private fun extractLibraryDescriptor(raw: String?): String? {
        raw ?: return null
        val trimmed = raw.trim()
        val match = bracketedLibraryRegex.find(trimmed)
        return match?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun descriptorToLibraryPath(descriptor: String): String {
        val parts = descriptor.split("@", limit = 2)
        val coords = parts[0].split(":")
        require(coords.size >= 3) { "非法的库坐标: $descriptor" }
        val group = coords[0].replace('.', '/')
        val artifact = coords[1]
        val version = coords[2]
        val classifier = coords.getOrNull(3)?.takeIf { it.isNotBlank() }
        val extension = parts.getOrNull(1)?.ifBlank { null } ?: "jar"
        val fileName = buildString {
            append(artifact).append('-').append(version)
            if (classifier != null) append('-').append(classifier)
            append('.').append(extension)
        }
        return "$group/$artifact/$version/$fileName"
    }

    private fun runInstallerBootstrapperLegacy(
        installBooter: File,
        installer: File,
        onProgress: (String) -> Unit
    ) {
        val classpathSeparator = if (hostOs.isWindows) ";" else ":"
        val classpath = listOf(installBooter.absolutePath, installer.absolutePath).joinToString(classpathSeparator)
        val command = listOf(
            javaExePath,
            "-cp",
            classpath,
            "com.bangbang93.ForgeInstaller",
            DIR.absolutePath,
        )
        val process = ProcessBuilder(command)
            .directory(DIR)
            .redirectErrorStream(true)
            .start()
        process.inputStream.bufferedReader(StandardCharsets.UTF_8).useLines { lines ->
            lines.forEach { line ->
                if (line.isNotBlank()) {
                    onProgress(line)
                }
            }
        }
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw IllegalStateException("Loader installation failed with code: $exitCode")
        } else
            onProgress("Loader installed successfully!")
    }

    private fun libraryKey(library: MojangLibrary): String {
        val artifactPath = library.downloads.artifact.path.orEmpty()
        val classifierKey = library.downloads.classifiers?.keys?.sorted()?.joinToString(";").orEmpty()
        return "${library.name}|$artifactPath|$classifierKey"
    }

    @Serializable
    private data class LoaderInstallProfile(
        val libraries: List<MojangLibrary> = emptyList(),
        val data: Map<String, LoaderInstallData> = emptyMap(),
    )

    @Serializable
    private data class LoaderInstallData(
        val client: String? = null,
        val server: String? = null,
    )

    private fun shouldDownloadAsset(path: String): Boolean {
        if (path == "minecraft/resourcepacks/programmer_art.zip") return false
        if (path.startsWith("realms/")) {
            return path == "realms/lang/en_us.json"
        }
        //只下载简中语言文件
        if (path.startsWith("minecraft/lang/")) {
            return path.equals("minecraft/lang/zh_cn.json", ignoreCase = true)
        }
        if (path.startsWith("minecraft/sounds/")) {
            val rest = path.removePrefix("minecraft/sounds/")
            if (rest.startsWith("records/") || rest.startsWith("music/") || rest.startsWith("ambient/")) {
                return false
            }
        }
        return true
    }

    private fun shouldUseEmptySound(path: String): Boolean {
        if (!path.endsWith(".ogg", ignoreCase = true)) return false
        if (!path.startsWith("minecraft/sounds/")) return false
        val rest = path.removePrefix("minecraft/sounds/")
        return rest.startsWith("records/") || rest.startsWith("music/") || rest.startsWith("ambient/")
    }

    private fun writeEmptySoundStub(hash: String) {
        val targetDir = assetObjectsDir.resolve(hash.substring(0, 2))
        val targetFile = targetDir.resolve(hash)
        if (targetFile.exists()) return
        targetDir.mkdirs()
        jarResource("assets/empty.ogg").use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun buildAssetUrl(hash: String): String {
        val base = "http://resources.download.minecraft.net".rewriteMirrorUrl
        val sub = hash.take(2)
        return "$base/$sub/$hash"
    }

    private data class NumberedAsset(
        val path: String,
        val asset: MojangAssetObject,
        val number: Int
    )

    private data class AssetLinkPlan(
        val fromHash: String,
        val toHash: String,
        val path: String
    )

    private fun mapEntry(path: String, asset: MojangAssetObject): Map.Entry<String, MojangAssetObject> {
        return object : Map.Entry<String, MojangAssetObject> {
            override val key: String = path
            override val value: MojangAssetObject = asset
        }
    }

    private fun createObjectLink(fromHash: String, toHash: String) {
        if (fromHash.equals(toHash, true)) return
        val fromDir = assetObjectsDir.resolve(fromHash.substring(0, 2))
        val toDir = assetObjectsDir.resolve(toHash.substring(0, 2))
        val fromFile = fromDir.resolve(fromHash)
        val toFile = toDir.resolve(toHash)
        if (fromFile.exists() || !toFile.exists()) return
        fromDir.mkdirs()
        runCatching {
            Files.createSymbolicLink(fromFile.toPath(), toFile.toPath())
        }.onFailure {
            runCatching {
                Files.copy(toFile.toPath(), fromFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }

    private fun isTooManyRequests(error: Throwable): Boolean {
        val message = error.message ?: return false
        return message.contains("429")
    }

    fun downloadLoader(version: McVersion, loader: ModLoader): Task {
        val holder = LoaderInstallHolder(version = version, loader = loader)
        return Task.Sequence(
            name = "安装 $loader",
            subTasks = listOf(
                Task.Leaf("下载$loader 安装器") { ctx ->
                    prepareInstaller(holder, ctx)
                },
                Task.Leaf("解析安装器") { ctx ->
                    parseInstaller(holder, ctx)
                },
                Task.Leaf("下载$loader 依赖") { ctx ->
                    downloadLibrariesTask(holder.loaderLibraries, ctx)
                },
                Task.Leaf("下载Mojmap") { ctx ->
                    downloadMojmapIfNeededTask(holder, ctx)
                },
                Task.Leaf("运行安装器") { ctx ->
                    runInstallerBootstrapperTask(holder, ctx)
                }
            ),
        )
    }

    suspend fun downloadLoaderLegacy(version: McVersion, loader: ModLoader, onProgress: (String) -> Unit) {
        val loaderMeta = version.loaderVersions[loader]
            ?: error("未配置 $loader 安装器下载链接")
        "launcher_profiles.json".let { File(DIR, it).apply { this.exportFromJarResource(it) } }
        val installBooter =
            "forge-install-bootstrapper.jar".let { File(DIR, it).apply { this.exportFromJarResource(it) } }
        val installer = DIR.resolve("${version.mcVer}-$loader-installer.jar")
        installer.parentFile?.mkdirs()
        onProgress("下载 $version $loader 安装器...")
        if (!installer.exists() || installer.sha1 != loaderMeta.installerSha1) {
            installer.toPath().downloadFileFrom(loaderMeta.installerUrl.rewriteMirrorUrl) { progress ->
                onProgress("$loader 安装器 ${progress.fraction.toFixed(2)}%")
            }
        }

        val vanillaManifest = version.metadata

        val versionJsonText = readInstallerEntry(installer, "version.json")
        val loaderVersionManifest = serdesJson.decodeFromString<MojangVersionManifest>(versionJsonText)
        val loaderVersionDir = versionListDir.resolve(loaderVersionManifest.id).apply { mkdirs() }
        File(loaderVersionDir, "${loaderVersionManifest.id}.json").writeText(loaderVersionManifest.json)

        val installProfileText = readInstallerEntry(installer, "install_profile.json")
        val installProfile = serdesJson.decodeFromString<LoaderInstallProfile>(installProfileText)
        val loaderLibraries = (installProfile.libraries + loaderVersionManifest.libraries)
            .distinctBy { libraryKey(it) }
        if (loaderLibraries.isNotEmpty()) {
            onProgress("下载 $loader 依赖 (${loaderLibraries.size}) ...")
            downloadLibrariesLegacy(loaderLibraries, onProgress)
        }

        downloadMojmapIfNeededLegacy(installProfile, vanillaManifest, onProgress)

        runInstallerBootstrapperLegacy(installBooter, installer, onProgress)
    }

    private data class LoaderInstallHolder(
        val version: McVersion,
        val loader: ModLoader,
        var installer: File? = null,
        var installBooter: File? = null,
        var loaderVersionManifest: MojangVersionManifest? = null,
        var installProfile: LoaderInstallProfile? = null,
        var loaderLibraries: List<MojangLibrary> = emptyList()
    )

    private suspend fun prepareInstaller(holder: LoaderInstallHolder, ctx: TaskContext) {
        val loaderMeta = holder.version.loaderVersions[holder.loader]
            ?: error("未配置 ${holder.loader} 安装器下载链接")
        "launcher_profiles.json".let { File(DIR, it).apply { this.exportFromJarResource(it) } }
        val installBooter =
            "forge-install-bootstrapper.jar".let { File(DIR, it).apply { this.exportFromJarResource(it) } }
        val installer = DIR.resolve("${holder.version.mcVer}-${holder.loader}-installer.jar")
        installer.parentFile?.mkdirs()
        holder.installBooter = installBooter
        holder.installer = installer

        if (installer.exists() && installer.sha1 == loaderMeta.installerSha1) {
            ctx.emitProgress(TaskProgress("安装器已存在", 1f))
            return
        }

        ctx.emitProgress(TaskProgress("开始下载...", 0f))
        installer.toPath().downloadFileFrom(loaderMeta.installerUrl.rewriteMirrorUrl) { progress ->
            ctx.emitProgress(
                TaskProgress(
                    "${progress.bytesDownloaded.humanFileSize}/${progress.totalBytes.humanFileSize}",
                    progress.fraction
                )
            )
        }.getOrThrow()
        ctx.emitProgress(TaskProgress("下载完成", 1f))
    }

    private fun parseInstaller(holder: LoaderInstallHolder, ctx: TaskContext) {
        val installer = holder.installer ?: error("安装器未准备")
        val versionJsonText = readInstallerEntry(installer, "version.json")
        val loaderVersionManifest = serdesJson.decodeFromString<MojangVersionManifest>(versionJsonText)
        val loaderVersionDir = versionListDir.resolve(loaderVersionManifest.id).apply { mkdirs() }
        File(loaderVersionDir, "${loaderVersionManifest.id}.json").writeText(loaderVersionManifest.json)

        val installProfileText = readInstallerEntry(installer, "install_profile.json")
        val installProfile = serdesJson.decodeFromString<LoaderInstallProfile>(installProfileText)
        val loaderLibraries = (installProfile.libraries + loaderVersionManifest.libraries)
            .distinctBy { libraryKey(it) }

        holder.loaderVersionManifest = loaderVersionManifest
        holder.installProfile = installProfile
        holder.loaderLibraries = loaderLibraries

        ctx.emitProgress(TaskProgress("解析完成", 1f))
    }

    private suspend fun downloadLibrariesTask(libraries: List<MojangLibrary>, ctx: TaskContext) {
        downloadLibraries(libraries).execute(ctx)
    }

    private suspend fun downloadSingleLibrary(library: MojangLibrary, ctx: TaskContext) {
        ctx.emitProgress(TaskProgress("开始下载...", 0f))
        downloadLibraryArtifact(library) { progress ->
            ctx.emitProgress(
                TaskProgress(
                    "${library.name} ${progress.bytesDownloaded.humanFileSize}/${progress.totalBytes.humanFileSize}",
                    progress.fraction
                )
            )
        }.getOrThrow()
        ctx.emitProgress(TaskProgress("下载完成", 1f))
    }

    private suspend fun downloadMojmapIfNeededTask(holder: LoaderInstallHolder, ctx: TaskContext) {
        val installProfile = holder.installProfile ?: return
        val vanillaManifest = holder.version.metadata
        val mojmaps = installProfile.data["MOJMAPS"] ?: return
        val downloads = vanillaManifest.downloads ?: return
        val tasks = mutableListOf<Triple<String, MojangDownloadArtifact, String>>()
        extractLibraryDescriptor(mojmaps.client)?.let { descriptor ->
            downloads.clientMappings?.let { artifact ->
                tasks += Triple("客户端", artifact, descriptor)
            }
        }
        extractLibraryDescriptor(mojmaps.server)?.let { descriptor ->
            downloads.serverMappings?.let { artifact ->
                tasks += Triple("服务端", artifact, descriptor)
            }
        }
        if (tasks.isEmpty()) {
            ctx.emitProgress(TaskProgress("无需下载", 1f))
            return
        }
        val total = tasks.size
        tasks.forEachIndexed { index, (label, artifact, descriptor) ->
            val relativePath = descriptorToLibraryPath(descriptor)
            val target = File(libsDir, relativePath)
            downloadArtifact(label, artifact, target) { progress ->
                ctx.emitProgress(
                    TaskProgress(
                        "$label ${progress.bytesDownloaded.humanFileSize}/${progress.totalBytes.humanFileSize}",
                        progress.fraction
                    )
                )
            }.getOrThrow()
            ctx.emitProgress(TaskProgress("已完成 ${index + 1}/$total", (index + 1).toFloat() / total))
        }
    }

    private fun runInstallerBootstrapperTask(holder: LoaderInstallHolder, ctx: TaskContext) {
        val installBooter = holder.installBooter ?: error("安装引导未准备")
        val installer = holder.installer ?: error("安装器未准备")
        val classpathSeparator = if (hostOs.isWindows) ";" else ":"
        val classpath = listOf(installBooter.absolutePath, installer.absolutePath).joinToString(classpathSeparator)
        val command = listOf(
            javaExePath,
            "-cp",
            classpath,
            "com.bangbang93.ForgeInstaller",
            DIR.absolutePath,
        )
        val process = ProcessBuilder(command)
            .directory(DIR)
            .redirectErrorStream(true)
            .start()
        process.inputStream.bufferedReader(StandardCharsets.UTF_8).useLines { lines ->
            lines.forEach { line ->
                if (line.isNotBlank()) {
                    ctx.emitProgress(TaskProgress(line, null))
                }
            }
        }
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            throw IllegalStateException("Loader installation failed with code: $exitCode")
        } else {
            ctx.emitProgress(TaskProgress("Loader installed successfully!", 1f))
        }
    }

    private suspend fun downloadArtifact(
        label: String,
        artifact: MojangDownloadArtifact,
        target: File,
        onProgress: (DownloadProgress) -> Unit
    ): Result<File> {
        if (target.exists()) {
            val existingSha = runCatching { target.sha1 }.getOrNull()
            if (existingSha != null && existingSha.equals(artifact.sha1, true)) {
                return Result.success(target)
            }
        }
        target.parentFile?.mkdirs()
        target.toPath().downloadFileFrom(
            url = artifact.url.rewriteMirrorUrl,
            knownSize = artifact.size
        ) { progress ->
            onProgress(progress)
        }.getOrElse {
            target.delete()
            throw it
        }
        val downloadedSha = target.sha1
        if (!downloadedSha.equals(artifact.sha1, true)) {
            target.delete()
            throw IllegalStateException("$label 校验失败")
        }
        return Result.success(target)
    }

    fun start(mcVer: McVersion, versionId: String, vararg jvmArgs: String, onLine: (String) -> Unit): Process {
        val loaderManifest = mcVer.loaderManifest
        val manifest = mcVer.manifest
        val nativesDir = mcVer.nativesDir
        val versionDir = versionListDir.resolve(versionId)
        val gameArgs = resolveArgumentList(manifest.arguments.game + loaderManifest.arguments.game).map {
            it.replace($$"${auth_player_name}", loggedAccount.name)
                .replace($$"${version_name}", versionId)
                .replace($$"${game_directory}", versionDir.absolutePath)
                .replace($$"${assets_root}", assetsDir.absolutePath)
                .replace($$"${assets_index_name}", manifest.assets!!)
                .replace($$"${auth_uuid}", loggedAccount._id.toUUID().toString().replace("-", ""))
                .replace($$"${auth_access_token}", loggedAccount.jwt ?: "")
                .replace($$"${user_type}", "msa")
                .replace($$"${version_type}", "RDI")
                .replace($$"${resolution_width}", "1280")
                .replace($$"${resolution_height}", "720")

        }
        val resolvedJvmArgs = resolveArgumentList(manifest.arguments.jvm + loaderManifest.arguments.jvm)
        val classpath = (manifest.buildClasspath() + loaderManifest.buildClasspath())
            .distinct()
            .toMutableList()
            .apply {
                this += versionListDir.resolve(versionId).resolve("$versionId.jar").absolutePath
            }
            .joinToString(File.pathSeparator)
        val useMemStr = runCatching {
            val osBean = ManagementFactory.getOperatingSystemMXBean()
            val freeBytes = (osBean as? OperatingSystemMXBean)
                ?.freeMemorySize
                ?: return@runCatching "-Xmx8G"
            val freeMb = freeBytes / (1024L * 1024)
            lgr.info { "剩余内存${freeBytes.humanFileSize}" }
            val memGb = if (freeMb > 8192) freeMb else 8192
            "-Xmx${memGb}M"
        }.getOrDefault("-Xmx8G")
        val processedJvmArgs = resolvedJvmArgs.map { arg ->
            arg.replace($$"${natives_directory}", nativesDir.absolutePath)
                .replace($$"${library_directory}", libsDir.absolutePath)
                .replace($$"${launcher_name}", "rdi")
                .replace($$"${launcher_version}", Const.VERSION_NUMBER)
                .replace($$"${classpath}", classpath)
                .replace($$"${classpath_separator}", File.pathSeparator)
        }.toMutableList().apply {
            this += useMemStr
            this += jvmArgs
        }

        lgr.info { "JVM Args: ${processedJvmArgs.joinToString(" ")}" }
        lgr.info { "Game Args: ${gameArgs.joinToString(" ")}" }
        val command = listOf(
            javaExePath,
            *processedJvmArgs.toTypedArray(),
            loaderManifest.mainClass,
            *gameArgs.toTypedArray(),
        )
        val process = ProcessBuilder(command)
            .directory(versionDir)
            .redirectErrorStream(true)
            .start()
        started = true
        thread(name = "mc-log-reader", isDaemon = true) {
            try {
                process.inputStream.bufferedReader(StandardCharsets.UTF_8).useLines { lines ->
                    lines.forEach { line ->
                        if (line.isNotBlank()) {
                            onLine(line)
                        }
                    }
                }
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    onLine("启动失败，退出代码: $exitCode")
                } else {
                    onLine("已退出")
                }
            } finally {
                started = false
            }
        }
        return process
    }

    private fun resolveArgumentList(source: List<JsonElement>): List<String> {
        val args = mutableListOf<String>()
        val ruleListSerializer = ListSerializer(MojangRule.serializer())
        source.forEach { element ->
            when (element) {
                is JsonPrimitive -> if (element.isString) args += element.content
                is JsonObject -> {
                    val rules = element["rules"]?.let { serdesJson.decodeFromJsonElement(ruleListSerializer, it) }
                    if (!rulesAllow(rules)) return@forEach
                    val valueElement = element["value"] ?: return@forEach
                    when (valueElement) {
                        is JsonPrimitive -> if (valueElement.isString) args += valueElement.content
                        is JsonArray -> valueElement.forEach { item ->
                            if (item is JsonPrimitive && item.isString) args += item.content
                        }

                        else -> {}
                    }
                }

                else -> {}
            }
        }
        return args
    }

    private fun rulesAllow(rules: List<MojangRule>?): Boolean {
        if (rules.isNullOrEmpty()) return true
        var allowed = false
        rules.forEach { rule ->
            if (rule.matchesHost()) {
                allowed = rule.action == MojangRuleAction.allow
            }
        }
        return allowed
    }

    private fun MojangRule.matchesHost(): Boolean {
        os?.let { spec ->
            val osName = spec.name
            if (osName != null && !hostOs.ruleOsName.equals(osName, true)) return false
            val archSpec = spec.arch?.lowercase(Locale.ROOT)
            if (archSpec != null && !hostOsArchRaw.contains(archSpec)) return false
            val versionSpec = spec.version
            if (versionSpec != null) {
                val regex = runCatching { Regex(versionSpec) }.getOrNull()
                val matches = regex?.containsMatchIn(hostOsVersionRaw) ?: hostOsVersionRaw.contains(versionSpec, true)
                if (!matches) return false
            }
        }
        val requiredFeatures = features ?: return true
        if (requiredFeatures.isEmpty()) return true
        return requiredFeatures.all { (feature, expected) ->
            launcherFeatures[feature] == expected
        }
    }

    private fun MojangVersionManifest.buildClasspath(): List<String> {
        val entries = this.libraries
            .mapNotNull { lib -> lib.downloads.artifact.path }
            .map { File(libsDir, it).absolutePath }
            .toMutableList()

            .distinct()
        return entries
    }
}
