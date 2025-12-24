package calebxzhou.rdi.common.service

import calebxzhou.mykotutils.log.Loggers
import calebxzhou.rdi.common.DL_MOD_DIR
import calebxzhou.rdi.common.RDI
import calebxzhou.rdi.common.model.ModBriefInfo
import calebxzhou.rdi.common.model.ModCardVo
import calebxzhou.rdi.common.serdesJson
import com.electronwill.nightconfig.core.CommentedConfig
import com.electronwill.nightconfig.core.Config
import com.electronwill.nightconfig.toml.TomlFormat
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.util.jar.JarFile
import java.util.jar.JarInputStream


object ModService {
    val briefInfo: List<ModBriefInfo> by lazy { loadBriefInfo() }
    const val NEOFORGE_CONFIG_PATH = "META-INF/neoforge.mods.toml"
    const val FABRIC_CONFIG_PATH = "fabric.mod.json"
    const val FORGE_CONFIG_PATH = "META-INF/mods.toml"
    private val lgr by Loggers

    val downloadedMods =DL_MOD_DIR.listFiles { it.extension == "jar" }?.toMutableList() ?: mutableListOf()
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

    fun ModBriefInfo.toVo(modFile: File? = null): ModCardVo {
        val iconBytes = modFile?.let {
            runCatching { JarFile(it).use { jar -> jar.modLogo } }.getOrNull()
        }
        return ModCardVo(
            name = name,
            nameCn = nameCn,
            intro = intro,
            iconData = iconBytes,
            iconUrls = buildList {
                if (logoUrl.isNotBlank()) add(logoUrl)
            }
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
        val missing: List<Missing>) {
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
                            val dependencyModId = dependency.get<String>("modId")?.trim()?.lowercase() ?: return@mapNotNull null
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
                                "Mod '$modId' is missing dependencies: ${missingDependencies.joinToString(", ") { missing ->
                                    missing.version?.let { ver -> "${missing.modId} ($ver)" } ?: missing.modId
                                }}"
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
                lgr.warn  ( "Failed to inspect nested jar '$name' inside ${jar.name}" )
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
            lgr.debug("Failed to parse nested neoforge config",err, )
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


}

