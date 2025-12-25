package calebxzhou.rdi.service

import calebxzhou.mykotutils.ktor.downloadFileFrom
import calebxzhou.mykotutils.log.Loggers
import calebxzhou.mykotutils.std.exportFromJarResource
import calebxzhou.mykotutils.std.javaExePath
import calebxzhou.mykotutils.std.sha1
import calebxzhou.mykotutils.std.toFixed
import calebxzhou.rdi.CONF
import calebxzhou.rdi.Const
import calebxzhou.rdi.RDI
import calebxzhou.rdi.common.json
import calebxzhou.rdi.common.model.LibraryOsArch.Companion.detectHostOs
import calebxzhou.rdi.common.model.ModLoader
import calebxzhou.rdi.common.net.httpRequest
import calebxzhou.rdi.common.serdesJson
import calebxzhou.rdi.common.util.toUUID
import calebxzhou.rdi.model.*
import calebxzhou.rdi.net.loggedAccount
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
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
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.zip.ZipFile

object GameService {
    private val lgr by Loggers
    val DIR = File(RDI.DIR, "mc").apply { mkdirs() }
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
        extractNatives(manifest, onProgress)
        downloadAssets(manifest, onProgress)
        onProgress("$version 所需文件下载完成")
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

    private suspend fun downloadClient(manifest: MojangVersionManifest, onProgress: (String) -> Unit) {
        val client = manifest.downloads?.client ?: return
        val versionDir = versionListDir.resolve(manifest.id).apply { mkdirs() }
        File(versionDir, "${manifest.id}.json").writeText(manifest.json)
        val target = File(versionDir, "${manifest.id}.jar")
        downloadArtifact("客户端核心 ${manifest.id}", client, target, onProgress)
    }

    private suspend fun loadBaseManifest(version: McVersion): MojangVersionManifest {
        val manifestFile = versionListDir.resolve(version.mcVer).resolve("${version.mcVer}.json")
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
                .filter { it.shouldDownloadByArch() }
                .apply { onProgress("需要下载的库文件: ${this.size}个： ${this.joinToString("\n") { it.name }}") }
                .map { library ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            downloadLibraryArtifacts(library, onProgress)
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
    private suspend fun downloadLibraryArtifacts(library: MojangLibrary, onProgress: (String) -> Unit): File {
        library.downloads.artifact
            .let { artifact ->
                val relativePath = artifact.path!!
                val target = File(libsDir, relativePath)
                downloadArtifact(library.name, artifact, target, onProgress)
                return target
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
        val success = target.toPath().downloadFileFrom(artifact.url.rewriteMirrorUrl) { progress ->
            val percent = progress.percent.takeIf { it >= 0 }
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

    private suspend fun downloadAssets(manifest: MojangVersionManifest, onProgress: (String) -> Unit) {
        val assetIndexMeta = manifest.assetIndex ?: let { onProgress("找不到资源"); return }
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
        val success =
            targetFile.toPath().downloadFileFrom(downloadUrl) { progress ->
                val percent = progress.percent.takeIf { it >= 0 }
                    ?.let { String.format(locale, "%.1f%%", it) }
                    ?: "--"
                onProgress("资源 $path 下载中 $percent")
            }.getOrElse {
                lastError = "下载失败: ${it.message ?: it::class.simpleName}"
                targetFile.delete()
                false
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
        val base = "http://resources.download.minecraft.net".rewriteMirrorUrl
        val sub = hash.take(2)
        return "$base/$sub/$hash"
    }

    suspend fun downloadLoader(version: McVersion, loader: ModLoader, onProgress: (String) -> Unit) {
        val loaderMeta = version.loaderVersions[loader]
            ?: error("未配置 $loader 安装器下载链接")
        "launcher_profiles.json".let { File(it).apply { this.exportFromJarResource(it) } }
        val installBooter = "forge-install-bootstrapper.jar".let { File(it).apply { this.exportFromJarResource(it) } }
        val installer = DIR.resolve("${version.mcVer}-$loader-installer.jar")
        installer.parentFile?.mkdirs()
        onProgress("下载 $version $loader 安装器...")
        if (!installer.exists() || installer.sha1 != loaderMeta.installerSha1) {
            installer.toPath().downloadFileFrom(loaderMeta.installerUrl.rewriteMirrorUrl) { progress ->
                onProgress("$loader 安装器 ${progress.percent.toFixed(2)}%")
            }
        }

        val vanillaManifest = loadBaseManifest(version)

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
            downloadLibraries(loaderLibraries, onProgress)
        }

        downloadMojmapIfNeeded(installProfile, vanillaManifest, onProgress)

        runInstallerBootstrapper(installBooter, installer, onProgress)
    }

    fun start(mcVer: McVersion, versionId: String, onProgress: (String) -> Unit) {
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

        val processedJvmArgs = resolvedJvmArgs.map { arg ->
            arg.replace($$"${natives_directory}", nativesDir.absolutePath)
                .replace($$"${library_directory}", libsDir.absolutePath)
                .replace($$"${launcher_name}", "rdi")
                .replace($$"${launcher_version}", Const.VERSION_NUMBER)
                .replace($$"${classpath}", classpath)
                .replace($$"${classpath_separator}", File.pathSeparator)
        }.toMutableList().apply { this += "-Xmx8G" }

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
        process.inputStream.bufferedReader(StandardCharsets.UTF_8).useLines { lines ->
            lines.forEach { line ->
                if (line.isNotBlank()) {
                    onProgress(line)
                }
            }
        }
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            onProgress("启动失败，退出代码: $exitCode")
        } else
            onProgress("已退出")
        // Actual process launch (auth, tokens, etc.) will be wired separately.
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