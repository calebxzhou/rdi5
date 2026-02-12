package calebxzhou.rdi.common.service

import calebxzhou.mykotutils.log.Loggers
import calebxzhou.mykotutils.std.murmur2
import calebxzhou.mykotutils.std.sha1
import calebxzhou.rdi.common.DL_MOD_DIR
import calebxzhou.rdi.common.model.*
import calebxzhou.rdi.common.net.DownloadProgress
import calebxzhou.rdi.common.net.downloadFileFrom
import calebxzhou.rdi.common.serdesJson
import com.electronwill.nightconfig.core.CommentedConfig
import com.electronwill.nightconfig.core.Config
import com.electronwill.nightconfig.toml.TomlFormat
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.nio.file.Path
import java.util.jar.JarFile
import java.util.jar.JarInputStream
import kotlin.io.path.exists


object ModService {
    var useMirror = true
    val briefInfo: List<ModBriefInfo> by lazy { loadBriefInfo() }
    const val NEOFORGE_CONFIG_PATH = "META-INF/neoforge.mods.toml"
    const val FABRIC_CONFIG_PATH = "fabric.mod.json"
    const val FORGE_CONFIG_PATH = "META-INF/mods.toml"
    private val lgr by Loggers

    val downloadedMods = DL_MOD_DIR.listFiles { it.extension == "jar" }?.toMutableList() ?: mutableListOf()
    var installedMods = DL_MOD_DIR.listFiles { it.extension == "jar" }?.toMutableList() ?: mutableListOf()
    fun JarFile.readNeoForgeConfig(): CommentedConfig? {
        return getJarEntry(NEOFORGE_CONFIG_PATH)?.let { modsTomlEntry ->
            getInputStream(modsTomlEntry).bufferedReader().use { reader ->
                val parsed = TomlFormat.instance()
                    .createParser()
                    .parse(reader.readText())
                parsed
            }
        }
    }

    val CommentedConfig.modId
        get() = get<List<Config>>("mods").firstOrNull()?.get<String>("modId")!!
    val CommentedConfig.modDescription
        get() = get<List<Config>>("mods").firstOrNull()?.get<String>("description")!!
    val JarFile.modLogo
        get() =
            getJarEntry("logo.png")?.let { logoEntry ->
                getInputStream(logoEntry).readBytes()
            }

    private val builtinDependencyIds = setOf("minecraft", "forge", "neoforge", "fabricloader")

    fun ModBriefInfo.toVo(modFile: File? = null): Mod.CardVo {
        val iconBytes = modFile?.let {
            runCatching { JarFile(it).use { jar -> jar.modLogo } }.getOrNull()
        }
        return Mod.CardVo(
            name = name,
            nameCn = nameCn,
            intro = intro,
            iconData = iconBytes,
            iconUrls = buildList {
                if (logoUrl.isNotBlank()) add(logoUrl)
            },
            side = Mod.Side.BOTH
        )
    }

    fun List<File>.filterServerOnlyMods() =
        filterNot { file ->
            JarFile(file).use { jar ->
                val config = jar.readNeoForgeConfig() ?: return@use false
                val modEntries = config.get("mods") as? List<Config> ?: return@use false

                modEntries.any { modConfig ->
                    val modId = modConfig.get<String>("modId")?.trim()?.lowercase() ?: return@any false
                    val dependencyKey = "dependencies.$modId"
                    val dependencies = config.get(dependencyKey) as? List<Config> ?: return@any false

                    dependencies.any { dependency ->
                        val dependencyModId = dependency.get<String>("modId")?.lowercase()
                        val side = dependency.get<String>("side")?.uppercase()
                        dependencyModId == "minecraft" && side == "CLIENT"
                    }
                }
            }
        }.toMutableList()

    data class UnmatchedDependencies(
        val modId: String,
        val missing: List<Missing>
    ) {
        data class Missing(val modId: String, val version: String? = null)
    }

    fun List<File>.checkDependencies(): List<UnmatchedDependencies> {
        val installedModIds = mutableSetOf<String>()
        this.forEach { file ->
            runCatching {
                JarFile(file).use { jar ->
                    collectModIdsFromJar(jar, installedModIds)
                }
            }.onFailure { err ->
                lgr.error("Failed to read mod id from file: ${file.name}", err)
            }
        }

        val unmatched = mutableListOf<UnmatchedDependencies>()

        this.forEach { file ->
            runCatching {
                JarFile(file).use { jar ->
                    val config = jar.readNeoForgeConfig() ?: return@use null
                    val modEntries = config.get("mods") as? List<Config> ?: return@use null

                    modEntries.forEach { modConfig ->
                        val modId = modConfig.get<String>("modId")?.trim()?.lowercase() ?: return@forEach
                        val dependencyKey = "dependencies.$modId"
                        val dependencies = config.get(dependencyKey) as? List<Config> ?: return@forEach

                        val missingDependencies = dependencies.mapNotNull { dependency ->
                            val dependencyModId =
                                dependency.get<String>("modId")?.trim()?.lowercase() ?: return@mapNotNull null
                            if (dependencyModId.isEmpty() || builtinDependencyIds.contains(dependencyModId)) return@mapNotNull null

                            val side = dependency.get<String>("side")?.trim()?.uppercase()
                            if (side == "CLIENT") return@mapNotNull null

                            val dependencyType = dependency.get<String>("type")?.trim()?.lowercase()
                            val optional = dependency.get<Boolean>("optional") ?: false
                            val mandatory = dependency.get<Boolean>("mandatory")
                            val required = when {
                                dependencyType == "optional" -> false
                                dependencyType == "incompatible" -> false
                                mandatory != null -> mandatory
                                optional -> false
                                dependencyType == "required" -> true
                                dependencyType == "discouraged" -> false
                                dependencyType == null -> true

                                else -> true
                            }
                            if (!required) return@mapNotNull null

                            if (installedModIds.contains(dependencyModId)) return@mapNotNull null

                            val versionRange = (dependency.get("versionRange") as? String)?.takeIf { it.isNotBlank() }
                            UnmatchedDependencies.Missing(dependencyModId, versionRange)
                        }
                            .distinctBy { it.modId }
                        if (missingDependencies.isNotEmpty()) {
                            lgr.warn(
                                "Mod '$modId' is missing dependencies: ${
                                    missingDependencies.joinToString(", ") { missing ->
                                        missing.version?.let { ver -> "${missing.modId} ($ver)" } ?: missing.modId
                                    }
                                }"
                            )
                            unmatched += UnmatchedDependencies(modId, missingDependencies)
                        }
                    }
                }
            }.onFailure { err ->
                lgr.error("Failed to check dependencies for mod file: ${file.name}", err)
            }
        }
        return unmatched
    }

    private fun collectModIdsFromJar(jar: JarFile, installedModIds: MutableSet<String>) {
        jar.readNeoForgeConfig()?.let { config ->
            installedModIds += extractModIds(config)
        }

        val entries = jar.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            if (entry.isDirectory) continue
            val name = entry.name
            if (!name.startsWith("META-INF/jarjar/", ignoreCase = true)) continue
            if (!name.endsWith(".jar", ignoreCase = true)) continue

            runCatching {
                jar.getInputStream(entry).use { nestedInput ->
                    collectModIdsFromNestedJar(nestedInput, installedModIds)
                }
            }.onFailure { err ->
                lgr.warn("Failed to inspect nested jar '$name' inside ${jar.name}")
                err.printStackTrace()
            }
        }
    }

    private fun collectModIdsFromNestedJar(inputStream: InputStream, installedModIds: MutableSet<String>) {
        JarInputStream(inputStream).use { nestedJar ->
            var entry = nestedJar.nextJarEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val entryName = entry.name
                    when {
                        entryName.equals(NEOFORGE_CONFIG_PATH, ignoreCase = false) -> {
                            val configText = nestedJar.readBytes().toString(Charsets.UTF_8)
                            parseNeoForgeConfig(configText)?.let { config ->
                                installedModIds += extractModIds(config)
                            }
                        }

                        entryName.startsWith("META-INF/jarjar/", ignoreCase = true) &&
                                entryName.endsWith(".jar", ignoreCase = true) -> {
                            val nestedBytes = nestedJar.readBytes()
                            collectModIdsFromNestedJar(ByteArrayInputStream(nestedBytes), installedModIds)
                        }
                    }
                }
                nestedJar.closeEntry()
                entry = nestedJar.nextJarEntry
            }
        }
    }

    private fun parseNeoForgeConfig(raw: String): CommentedConfig? {
        if (raw.isBlank()) return null
        return runCatching {
            TomlFormat.instance().createParser().parse(raw)
        }.onFailure { err ->
            lgr.debug("Failed to parse nested neoforge config", err)
        }.getOrNull()
    }

    private fun extractModIds(config: Config?): List<String> {
        val modEntries = config?.get("mods") as? List<Config> ?: return emptyList()
        return modEntries.mapNotNull { modConfig ->
            modConfig.get<String>("modId")
                ?.trim()
                ?.lowercase()
                ?.takeIf { it.isNotEmpty() }
        }
    }


    val String.ofMirrorUrl
        get() = this.replace("edge.forgecdn.net", "mod.mcimirror.top")
            .replace("mediafilez.forgecdn.net", "mod.mcimirror.top")
            .replace("media.forgecdn.net", "mod.mcimirror.top")
            .replace("api.modrinth.com", "mod.mcimirror.top/modrinth")
            .replace("staging-api.modrinth.com", "mod.mcimirror.top/modrinth")
            .replace("cdn.modrinth.com", "mod.mcimirror.top")
            .replace("api.curseforge.com", "mod.mcimirror.top/curseforge")

    fun loadBriefInfo(): List<ModBriefInfo> {
        val resourcePath = "mod_brief_info.json"
        val raw = runCatching {
            ModService::class.java.classLoader.getResourceAsStream(resourcePath)?.bufferedReader()
                ?.use { it.readText() }
        }.onFailure {
            lgr.error(it) { "Failed to read $resourcePath" }
        }.getOrNull()

        if (raw.isNullOrBlank()) {
            lgr.warn { "mod_brief_info.json is missing or empty; fallback to empty brief info list" }
            return emptyList()
        }

        return runCatching { serdesJson.decodeFromString<List<ModBriefInfo>>(raw) }
            .onFailure { err -> lgr.error(err) { "Failed to decode mod_brief_info.json" } }
            .getOrElse { emptyList() }
    }

    fun buildSlugMap(
        data: List<ModBriefInfo>,
        slugSelector: (ModBriefInfo) -> List<String>
    ): Map<String, ModBriefInfo> {
        val map = linkedMapOf<String, ModBriefInfo>()
        data.forEach { info ->
            slugSelector(info)
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { slug ->
                    val normalized = slug.lowercase()
                    val previous = map.put(normalized, info)
                    if (previous != null && previous !== info) {
                        lgr.debug { "Duplicated slug '$slug' now mapped to ${info.mcmodId}, previously ${previous.mcmodId}" }
                    }
                }
        }
        return map
    }

    fun downloadModsTask(mods: List<Mod>): Task {
        if (mods.isEmpty()) return Task.Group("下载Mod", emptyList())
        val cfMods = mods.filter { it.platform == "cf" }
        val mrMods = mods.filter { it.platform == "mr" }
        val tasks = buildList {
            add(downloadCFModsTask(cfMods))
            add(downloadMRModsTask(mrMods))
        }
        return Task.Sequence("下载Mod", tasks)
    }

    fun downloadCFModsTask(mods: List<Mod>): Task {
        if (mods.isEmpty()) return Task.Group("下载CurseForge Mod", emptyList())
        val fileIds = mods.map { it.fileId.toInt() }
        val fileInfoMap = mutableMapOf<Int, CurseForgeFile>()
        val prepareTask = Task.Leaf("获取CurseForge文件信息") { ctx ->
            val fileInfos = CurseForgeService.getModFilesInfo(fileIds)
            fileInfoMap.clear()
            fileInfoMap.putAll(fileInfos.associateBy { it.id })
            ctx.emitProgress(TaskProgress("获取完成", 1f))
        }
        val tasks = mods.map { mod ->
            Task.Leaf("下载 ${mod.slug}") { ctx ->
                val fileInfo = fileInfoMap[mod.fileId.toInt()]
                    ?: throw IllegalStateException("未找到文件信息: ${mod.slug}")
                val result = downloadSingleCFMod(mod, fileInfo, 4) { progress ->
                    ctx.emitProgress(
                        TaskProgress(
                            "Mod下载中 ${mod.slug}",
                            progress.fraction.coerceIn(0f, 1f)
                        )
                    )
                }
                result.getOrElse { throw it }
            }
        }
        return Task.Sequence("下载CurseForge Mod", listOf(prepareTask, Task.Group("下载CurseForge Mod", tasks)))
    }

    fun downloadMRModsTask(mods: List<Mod>): Task {
        if (mods.isEmpty()) return Task.Group("下载Modrinth Mod", emptyList())
        val modsWithUrls = mods.filter { it.downloadUrls.isNotEmpty() }
        val tasks = modsWithUrls.map { mod ->
            Task.Leaf("下载 ${mod.slug}") { ctx ->
                val result = downloadSingleMRMod(mod) { progress ->
                    ctx.emitProgress(
                        TaskProgress(
                            "Mod下载中 ${mod.slug}",
                            progress.fraction.coerceIn(0f, 1f)
                        )
                    )
                }
                result.getOrElse { throw it }
            }
        }
        return Task.Group("下载Modrinth Mod", tasks)
    }

    private suspend fun downloadSingleCFMod(
        mod: Mod,
        fileInfo: CurseForgeFile,
        rangeParallelism: Int,
        onProgress: (DownloadProgress) -> Unit
    ): Result<Path> {
        val targetPath = mod.targetPath
        val expectedFingerprint = fileInfo.fileFingerprint

        // Check if file already exists with correct fingerprint
        if (targetPath.exists() && targetPath.murmur2 == expectedFingerprint) {
            lgr.debug { "Mod file already exists and fingerprint matches: $targetPath" }
            return Result.success(targetPath)
        }

        val officialUrl = fileInfo.realDownloadUrl
        val mirrorUrl = if (useMirror) {
            officialUrl.ofMirrorUrl
        } else {
            officialUrl
        }

        suspend fun attemptDownload(url: String, label: String): Result<Path> = runCatching {
            val downloadedPath = targetPath.downloadFileFrom(
                url,
                onProgress = onProgress
            ).getOrElse { throw it }

            // Verify fingerprint after download
            if (downloadedPath.murmur2 != expectedFingerprint) {
                throw IllegalStateException(
                    "Downloaded mod ${mod.slug} fingerprint mismatch: expected $expectedFingerprint, got ${downloadedPath.murmur2}"
                )
            }
            downloadedPath
        }.onFailure { err ->
            if (label == "mirror") {
                lgr.warn(err) { "Mirror download failed for ${mod.slug}, will retry official" }
            }
        }

        // Try mirror first if enabled, then fall back to official
        val mirrorResult = if (useMirror) attemptDownload(mirrorUrl, "mirror") else null
        val finalResult = when {
            mirrorResult == null -> attemptDownload(officialUrl, "official")
            mirrorResult.isSuccess -> mirrorResult
            else -> attemptDownload(officialUrl, "official")
        }

        finalResult.onFailure { err ->
            lgr.error(err) { "Failed to download mod ${mod.slug}" }
        }

        return finalResult
    }

    private suspend fun downloadSingleMRMod(
        mod: Mod,
        onProgress: (DownloadProgress) -> Unit
    ): Result<Path> {
        val targetPath = mod.targetPath

        // Check if file already exists with correct hash
        if (targetPath.exists()) {
            // For MR mods, we use the hash from the mod object
            val expectedHash = mod.hash
            if (expectedHash.isNotBlank()) {
                val actualHash = targetPath.sha1
                if (actualHash == expectedHash) {
                    lgr.debug { "Mod file already exists and hash matches: $targetPath" }
                    return Result.success(targetPath)
                }
            }
        }

        val urls = mod.downloadUrls
        val expectedHash = mod.hash
        var lastError: Throwable? = null

        // Try each URL in order until one succeeds
        for ((index, url) in urls.withIndex()) {
            val result = runCatching {
                val downloadedPath = targetPath.downloadFileFrom(
                    if(useMirror) url.ofMirrorUrl else url,
                    onProgress = onProgress
                ).getOrElse { throw it }

                // Verify SHA1 hash after download
                if (expectedHash.isNotBlank()) {
                    val actualHash = downloadedPath.sha1
                    if (actualHash != expectedHash) {
                        throw IllegalStateException(
                            "Downloaded mod ${mod.slug} SHA1 mismatch: expected $expectedHash, got $actualHash"
                        )
                    }
                }
                downloadedPath
            }

            if (result.isSuccess) {
                lgr.debug { "Successfully downloaded ${mod.slug} from URL #${index + 1}" }
                return result
            }

            lastError = result.exceptionOrNull()
            lgr.warn(lastError) { "Download failed for ${mod.slug} from URL #${index + 1}: $url" }

            // If there are more URLs to try, continue
            if (index < urls.lastIndex) {
                lgr.info { "Trying next URL for ${mod.slug}..." }
            }
        }

        // All URLs failed
        lgr.error(lastError) { "Failed to download mod ${mod.slug} from all ${urls.size} URLs" }
        return Result.failure(lastError ?: IllegalStateException("No download URLs available"))
    }

    fun MutableList<Mod>.postProcessModSides(): MutableList<Mod> {
        if (isEmpty()) return this

        val forceBoth = setOf(
            "loot-beams-refork",
            "particular-reforged",
            "inventory-profiles-next",
            "inventory-tweaks-refoxed",
            "just-enough-resources-jer",
            "radiant-gear",
            "fusion-connected-textures",
            "apothic-attributes",
            "the-twilight-forest",
        )
        val forceClient = setOf(
            "status-effect-bars-reforged",
            "mafglib",
            "flighthud-reborn",
            "i18nupdatemod",
            "modern-ui",
            "controllable"
        )

        forEach { mod ->
            val slug = mod.slug.trim().lowercase()
            when {
                slug.startsWith("ftb") -> mod.side = Mod.Side.BOTH
                slug in forceBoth -> mod.side = Mod.Side.BOTH
                slug in forceClient -> mod.side = Mod.Side.CLIENT
            }
            mod.vo = mod.vo?.copy(side = mod.side)
        }
        return this
    }
}

