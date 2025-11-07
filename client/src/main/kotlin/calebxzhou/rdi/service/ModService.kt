package calebxzhou.rdi.service

import calebxzhou.rdi.lgr
import calebxzhou.rdi.model.ModBriefInfo
import calebxzhou.rdi.model.ModCardVo
import calebxzhou.rdi.service.ModService.briefInfo
import calebxzhou.rdi.util.serdesJson
import com.electronwill.nightconfig.core.CommentedConfig
import com.electronwill.nightconfig.core.Config
import com.electronwill.nightconfig.toml.TomlFormat
import java.io.File
import java.util.jar.JarFile
import kotlin.collections.firstOrNull

const val NEOFORGE_CONFIG_PATH = "META-INF/neoforge.mods.toml"
const val FABRIC_CONFIG_PATH = "fabric.mod.json"
const val FORGE_CONFIG_PATH = "META-INF/mods.toml"

val MOD_DIR = System.getProperty("rdi.modDir")?.let { File(it) } ?: File("mods")
var installedMods = MOD_DIR.listFiles { it.extension == "jar" }?.toMutableList() ?: mutableListOf()
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

val CurseForgeService.slugBriefInfo: Map<String, ModBriefInfo> by lazy { ModService.buildSlugMap(briefInfo) { it.curseforgeSlugs } }
val ModrinthService.slugBriefInfo: Map<String, ModBriefInfo> by lazy { ModService.buildSlugMap(briefInfo) { it.modrinthSlugs } }

object ModService {
    val briefInfo: List<ModBriefInfo> by lazy { loadBriefInfo() }


    fun loadBriefInfo(): List<ModBriefInfo> {
        val resourcePath = "mod_brief_info.json"
        val raw = runCatching {
            ModService::class.java.classLoader.getResourceAsStream(resourcePath)?.bufferedReader()
                ?.use { it.readText() }
        }.onFailure {
            lgr.error("Failed to read $resourcePath", it)
        }.getOrNull()

        if (raw.isNullOrBlank()) {
            lgr.warn("mod_brief_info.json is missing or empty; fallback to empty brief info list")
            return emptyList()
        }

        return runCatching { serdesJson.decodeFromString<List<ModBriefInfo>>(raw) }
            .onFailure { err -> lgr.error("Failed to decode mod_brief_info.json", err) }
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
                        lgr.debug("Duplicated slug '$slug' now mapped to ${info.mcmodId}, previously ${previous.mcmodId}")
                    }
                }
        }
        return map
    }


}

