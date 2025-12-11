package calebxzhou.rdi.service

import calebxzhou.rdi.CONF
import calebxzhou.rdi.RDI
import calebxzhou.rdi.model.McVersion
import calebxzhou.rdi.model.ModLoader
import calebxzhou.rdi.model.MojangAssetIndex
import calebxzhou.rdi.model.MojangAssetIndexFile
import calebxzhou.rdi.model.MojangAssetObject
import calebxzhou.rdi.model.MojangDownloadArtifact
import calebxzhou.rdi.model.MojangLibrary
import calebxzhou.rdi.model.MojangLibraryDownloads
import calebxzhou.rdi.model.MojangRule
import calebxzhou.rdi.model.MojangRuleAction
import calebxzhou.rdi.model.MojangRuleOs
import calebxzhou.rdi.model.MojangVersionManifest
import calebxzhou.rdi.net.downloadFileWithProgress
import calebxzhou.rdi.net.httpRequest
import calebxzhou.rdi.util.Loggers
import calebxzhou.rdi.util.exportJarResource
import calebxzhou.rdi.util.javaExePath
import calebxzhou.rdi.util.json
import calebxzhou.rdi.util.serdesJson
import calebxzhou.rdi.util.sha1
import calebxzhou.rdi.util.toFixed
import io.ktor.client.call.body
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.Locale
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable

object GameService {
    private val lgr by Loggers
    private val DIR = File(RDI.DIR, "mc").apply { mkdirs() }
    private val libsDir = DIR.resolve("libraries").apply { mkdirs() }
    private val assetsDir = DIR.resolve("assets").apply { mkdirs() }
    private val assetIndexesDir = assetsDir.resolve("indexes").apply { mkdirs() }
    private val assetObjectsDir = assetsDir.resolve("objects").apply { mkdirs() }
    private val versionsDir = DIR.resolve("versions").apply { mkdirs() }
    private val hostOs = detectHostOs()
    private val launcherFeatures: Map<String, Boolean> = emptyMap()
    private val locale = Locale.SIMPLIFIED_CHINESE
    private const val MAX_PARALLEL_LIBRARY_DOWNLOADS = 6
    private const val MAX_PARALLEL_ASSET_DOWNLOADS = 32
    private val mirrors = mapOf(
        "https://maven.neoforged.net/releases/net/neoforged/forge" to "https://bmclapi2.bangbang93.com/maven/net/neoforged/forge",
        "https://maven.neoforged.net/releases/net/neoforged/neoforge" to " https://bmclapi2.bangbang93.com/maven/net/neoforged/neoforge",
        "https://files.minecraftforge.net/maven" to "https://bmclapi2.bangbang93.com/maven",
        "http://launchermeta.mojang.com/mc/game/version_manifest.json" to "https://bmclapi2.bangbang93.com/mc/game/version_manifest.json",
        "http://launchermeta.mojang.com/mc/game/version_manifest_v2.json" to "https://bmclapi2.bangbang93.com/mc/game/version_manifest_v2.json",
        "https://launchermeta.mojang.com" to "https://bmclapi2.bangbang93.com",
        "https://launcher.mojang.com" to "https://bmclapi2.bangbang93.com",
        "http://resources.download.minecraft.net" to "https://bmclapi2.bangbang93.com/assets",
        "https://libraries.minecraft.net" to "https://bmclapi2.bangbang93.com/maven",
        )
    private val bracketedLibraryRegex = Regex("^\\[(.+)]$")
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

    suspend fun downloadVersion(version: McVersion, onProgress: (String) -> Unit) {
        val manifestUrl = version.metaUrl
        onProgress("正在获取 ${version.mcVer} 的版本信息...")
        val manifest = httpRequest { url(manifestUrl) }.body<MojangVersionManifest>()
        downloadClient(manifest, onProgress)
        downloadLibraries(manifest, onProgress)
        downloadAssets(manifest, onProgress)
        onProgress("$version 所需文件下载完成")
    }


    private suspend fun downloadClient(manifest: MojangVersionManifest, onProgress: (String) -> Unit) {
        val client = manifest.downloads?.client ?: return
        val versionDir = versionsDir.resolve(manifest.id).apply { mkdirs() }
        File(versionDir, "${manifest.id}.json").writeText(manifest.json)
        val target = File(versionDir, "${manifest.id}.jar")
        downloadArtifact("客户端核心 ${manifest.id}", client, target, onProgress)
    }

    private suspend fun loadBaseManifest(version: McVersion): MojangVersionManifest {
        val manifestFile = versionsDir.resolve(version.mcVer).resolve("${version.mcVer}.json")
        val localJson = runCatching { manifestFile.takeIf { it.exists() }?.readText() }.getOrNull()
        if (!localJson.isNullOrBlank()) {
            return serdesJson.decodeFromString(localJson)
        }
        return httpRequest { url(version.metaUrl) }.body()
    }

    private suspend fun downloadLibraries(manifest: MojangVersionManifest, onProgress: (String) -> Unit) {
        downloadLibraries(manifest.libraries, onProgress)
    }

    private suspend fun downloadLibraries(libraries: List<MojangLibrary>, onProgress: (String) -> Unit) =
        coroutineScope {
            val semaphore = Semaphore(MAX_PARALLEL_LIBRARY_DOWNLOADS)
            libraries
                .filter { it.isAllowed(hostOs) }
                .map { library ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            downloadLibraryArtifacts(library, onProgress)
                        }
                    }
                }.awaitAll()
        }

    private suspend fun downloadLibraryArtifacts(library: MojangLibrary, onProgress: (String) -> Unit) {
        library.downloads.artifact?.let { artifact ->
            val relativePath = artifact.path ?: library.derivePath()
            val target = File(libsDir, relativePath)
            downloadArtifact(library.name, artifact, target, onProgress)
        }

        val classifier = library.downloads.classifierFor(hostOs)
        classifier?.let { nativeArtifact ->
            val relativePath = nativeArtifact.path ?: library.derivePath(suffix = "natives")
            val target = File(libsDir, relativePath)
            downloadArtifact("${library.name} (natives)", nativeArtifact, target, onProgress)
        }
    }

    private suspend fun downloadArtifact(
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
        val success = downloadFileWithProgress(artifact.url.rewriteMirrorUrl, target.toPath()) { progress ->
            val percent = progress.percent.takeIf { it >= 0 }
                ?.let { String.format(locale, "%.1f%%", it) }
                ?: "--"
            onProgress("$label 下载中 $percent")
        }
        if (!success) {
            target.delete()
            throw IllegalStateException("下载 $label 失败")
        }
        val downloadedSha = target.sha1
        if (!downloadedSha.equals(artifact.sha1, true)) {
            target.delete()
            throw IllegalStateException("$label 校验失败")
        }
    }

    private fun MojangLibrary.isAllowed(os: HostOs): Boolean {
        val ruleSet = rules ?: return true
        var allowed = true
        ruleSet.forEach { rule ->
            if (rule.applies(os)) {
                allowed = rule.action == MojangRuleAction.allow
            }
        }
        return allowed
    }

    private fun MojangRule.applies(os: HostOs): Boolean {
        if (!osMatches(os, this.os)) return false
        if (!featuresMatch(features)) return false
        return true
    }

    private fun osMatches(os: HostOs, osSpec: MojangRuleOs?): Boolean {
        osSpec ?: return true
        osSpec.name?.let { if (!os.name.equals(it, true)) return false }
        osSpec.arch?.let { if (!os.arch.equals(it, true)) return false }
        osSpec.version?.let {
            val regex = runCatching { Regex(it) }.getOrNull()
            if (regex != null) {
                if (!regex.containsMatchIn(os.version)) return false
            } else if (!os.version.contains(it, true)) {
                return false
            }
        }
        return true
    }

    private fun featuresMatch(required: Map<String, Boolean>?): Boolean {
        required ?: return true
        if (required.isEmpty()) return true
        return required.all { (key, value) -> launcherFeatures[key] == value }
    }

    private fun MojangLibraryDownloads.classifierFor(os: HostOs): MojangDownloadArtifact? {
        val map = classifiers ?: return null
        val archSuffix = if (os.arch.contains("64")) "-64" else ""
        val candidates = listOf(
            "natives-${os.name}$archSuffix",
            "natives-${os.name}",
            os.name,
        )
        candidates.forEach { candidate ->
            map[candidate]?.let { return it }
        }
        return null
    }

    private fun MojangLibrary.derivePath(suffix: String? = null): String {
        val parts = name.split(":")
        if (parts.size < 3) return name.replace(':', '/') + (suffix?.let { "-$it" } ?: "") + ".jar"
        val (group, artifact, version) = parts
        val classifierSuffix = suffix?.let { "-$it" } ?: ""
        val base = group.replace('.', '/')
        return "$base/$artifact/$version/$artifact-$version$classifierSuffix.jar"
    }

    private data class HostOs(val name: String, val arch: String, val version: String)

    private fun detectHostOs(): HostOs {
        val osName = System.getProperty("os.name")?.lowercase(Locale.ROOT) ?: ""
        val normalizedName = when {
            osName.contains("win") -> "windows"
            osName.contains("mac") || osName.contains("os x") -> "osx"
            else -> "linux"
        }
        val rawArch = System.getProperty("os.arch")?.lowercase(Locale.ROOT) ?: ""
        val normalizedArch = when {
            rawArch.contains("arm") && rawArch.contains("64") -> "arm64"
            rawArch.contains("arm") -> "arm"
            rawArch.contains("64") -> "x86_64"
            rawArch.contains("86") -> "x86"
            else -> rawArch
        }
        val version = System.getProperty("os.version") ?: ""
        return HostOs(normalizedName, normalizedArch, version)
    }


    private suspend fun downloadAssets(manifest: MojangVersionManifest, onProgress: (String) -> Unit) {
        val assetIndexMeta = manifest.assetIndex?: let { onProgress("找不到资源") ;return }
        val indexFile = assetIndexesDir.resolve("${assetIndexMeta.id}.json")
        val indexJson = fetchAssetIndex(assetIndexMeta, indexFile, onProgress)
        val index = serdesJson.decodeFromString<MojangAssetIndexFile>(indexJson)
        val filteredEntries = index.objects.entries.filter { shouldDownloadAsset(it.key) }
        onProgress("准备下载资源 (${filteredEntries.size}/${index.objects.size}) ...")

        val errors = Collections.synchronizedList(mutableListOf<String>())
        supervisorScope {
            val semaphore = Semaphore(MAX_PARALLEL_ASSET_DOWNLOADS)
            filteredEntries.map { (path, obj) ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        runCatching {
                            downloadAssetObject(path, obj, onProgress)
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

    private suspend fun fetchAssetIndex(
        meta: MojangAssetIndex,
        targetFile: File,
        onProgress: (String) -> Unit
    ): String {
        if (targetFile.exists()) {
            val existingSha = runCatching { targetFile.sha1 }.getOrNull()
            if (existingSha != null && existingSha.equals(meta.sha1, true)) {
                return targetFile.readText()
            }
        }
        onProgress("下载资源索引 ${meta.id} ...")
        val response = httpRequest { url(meta.url) }
        val text = response.bodyAsText()
        targetFile.parentFile?.mkdirs()
        targetFile.writeText(text)
        return text
    }

    private suspend fun downloadAssetObject(
        path: String,
        asset: MojangAssetObject,
        onProgress: (String) -> Unit
    ) {
        val hash = asset.hash.lowercase(Locale.ROOT)
        val targetDir = assetObjectsDir.resolve(hash.substring(0, 2))
        val targetFile = targetDir.resolve(hash)
        if (targetFile.exists()) {
            val existingSha = runCatching { targetFile.sha1 }.getOrNull()
            if (existingSha != null && existingSha.equals(hash, true)) {
                onProgress("跳过资源 $path (已存在)")
                return
            }
        }
        targetDir.mkdirs()
        var lastError: String? = null
        var completed = false
            val downloadUrl = buildAssetUrl(hash)
            onProgress("下载资源 $path ...")
            val success = try {
                downloadFileWithProgress(downloadUrl, targetFile.toPath()) { progress ->
                    val percent = progress.percent.takeIf { it >= 0 }
                        ?.let { String.format(locale, "%.1f%%", it) }
                        ?: "--"
                    onProgress("资源 $path 下载中 $percent")
                }
            } catch (ex: Exception) {
                lastError = "异常: ${ex.message ?: ex::class.simpleName}"
                targetFile.delete()
            }
            if (!success) {
                lastError = "下载失败"
                targetFile.delete()
            }
            val downloadedSha = targetFile.sha1
            if (!downloadedSha.equals(hash, true)) {
                lastError = "校验失败"
                targetFile.delete()
            }
            completed = true

        if (!completed) {
            throw IllegalStateException(lastError ?: "资源 $path 下载失败")
        }
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

    private suspend fun downloadMojmapIfNeeded(
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
            downloadArtifact("$label Mojmap", artifact, target, onProgress)
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

    private fun runInstallerBootstrapper(
        installBooter: File,
        installer: File,
        onProgress: (String) -> Unit
    ) {
        val classpathSeparator = if (hostOs.name == "windows") ";" else ":"
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
        }else
        onProgress("Loader installed successfully!")
    }

    private fun libraryKey(library: MojangLibrary): String {
        val artifactPath = library.downloads.artifact?.path.orEmpty()
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
        if (path.startsWith("realms/")) return false
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

    private fun buildAssetUrl(hash: String): String {
        val base = "http://resources.download.minecraft.net"
        val sub = hash.take(2)
        return "$base/$sub/$hash"
    }

    suspend fun downloadLoader(version: McVersion, loader: ModLoader, onProgress: (String) -> Unit) {
        val loaderMeta = version.loaderInstallerUrl[loader]
            ?: error("未配置 $loader 安装器下载链接")
        exportJarResource("launcher_profiles.json")
        val installBooter = exportJarResource("forge-install-bootstrapper.jar")
        val installer = DIR.resolve("${version.mcVer}-$loader-installer.jar")
        installer.parentFile?.mkdirs()
        onProgress("下载 $version $loader 安装器...")
        if (!installer.exists() || installer.sha1 != loaderMeta.installerSha1) {
            downloadFileWithProgress(loaderMeta.installerUrl.rewriteMirrorUrl, installer.toPath()) { progress ->
                onProgress("$loader 安装器 ${progress.percent.toFixed(2)}%")
            }
        }

        val vanillaManifest = loadBaseManifest(version)

        val versionJsonText = readInstallerEntry(installer, "version.json")
        val loaderVersionManifest = serdesJson.decodeFromString<MojangVersionManifest>(versionJsonText)
        val loaderVersionDir = versionsDir.resolve(loaderVersionManifest.id).apply { mkdirs() }
        File(loaderVersionDir, "${loaderVersionManifest.id}.json").writeText(loaderVersionManifest.json)

        val installProfileText = readInstallerEntry(installer, "install_profile.json")
        val installProfile = serdesJson.decodeFromString<LoaderInstallProfile>(installProfileText)
        val loaderLibraries = (installProfile.libraries + loaderVersionManifest.libraries)
            .distinctBy { libraryKey(it) }
        if (loaderLibraries.isNotEmpty()) {
            onProgress("下载 $loader 依赖 (${loaderLibraries.size}) ...")
            downloadLibraries(loaderLibraries, onProgress)
        }

        downloadMojmapIfNeeded(installProfile, vanillaManifest, onProgress)

        runInstallerBootstrapper(installBooter, installer, onProgress)
    }
}