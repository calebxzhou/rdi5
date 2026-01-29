package calebxzhou.rdi.common.service

import calebxzhou.mykotutils.modrinth.ModrinthApi
import calebxzhou.mykotutils.modrinth.ModrinthProject
import calebxzhou.rdi.common.model.ModBriefInfo
import calebxzhou.rdi.common.model.ModrinthModpackIndex
import calebxzhou.rdi.common.service.ModService.briefInfo
import calebxzhou.mykotutils.std.openChineseZip
import calebxzhou.rdi.common.exception.ModpackException
import calebxzhou.rdi.common.model.McVersion
import calebxzhou.rdi.common.model.Mod
import calebxzhou.rdi.common.model.ModLoader
import calebxzhou.rdi.common.serdesJson
import calebxzhou.rdi.common.service.ModService.modDescription
import calebxzhou.rdi.common.service.ModService.modLogo
import calebxzhou.rdi.common.service.ModService.readNeoForgeConfig
import java.io.File
import java.util.jar.JarFile

object ModrinthService {
    val slugBriefInfo: Map<String, ModBriefInfo> by lazy { ModService.buildSlugMap(briefInfo) { it.modrinthSlugs } }
    //mr - cf slug, 没查到就返回自身
    val String.mr2CfSlug: String
        get() {
            if (isBlank()) return this
            val info = slugBriefInfo[trim().lowercase()] ?: return this
            return info.curseforgeSlugs.firstOrNull { it.isNotBlank() }?.trim() ?: this
        }

    data class LoadedModpack(
        val index: ModrinthModpackIndex,
        val file: File,
        val mods: List<Mod>,
        val mcVersion: McVersion,
        val modloader: ModLoader
    )

    suspend fun loadModpack(modpackFile: File): Result<LoadedModpack> {
        if (!modpackFile.exists() || !modpackFile.isFile) {
            throw ModpackException("找不到整合包文件: ${modpackFile.path}")
        }
        val index = modpackFile.openChineseZip().use { zip ->
            val indexEntry = zip.entries().asSequence().firstOrNull {
                !it.isDirectory && it.name.substringAfterLast('/') == "modrinth.index.json"
            } ?: throw ModpackException("整合包缺少文件：modrinth.index.json")
            val indexJson = zip.getInputStream(indexEntry).bufferedReader(Charsets.UTF_8).use { it.readText() }
            runCatching {
                serdesJson.decodeFromString<ModrinthModpackIndex>(indexJson)
            }.getOrElse { err ->
                throw ModpackException("modrinth.index.json 解析失败: ${err.message}")
            }
        }

        if (!index.game.equals("minecraft", ignoreCase = true)) {
            throw ModpackException("不支持的游戏类型: ${index.game}")
        }
        if (index.formatVersion <= 0) {
            throw ModpackException("不支持的整合包格式版本: ${index.formatVersion}")
        }
        val mcVersion = index.dependencies["minecraft"]?.trim().orEmpty()
        if (mcVersion.isBlank()) {
            throw ModpackException("整合包缺少 minecraft 版本")
        }
        val parsedMcVersion = McVersion.from(mcVersion)
        if (parsedMcVersion == null) {
            throw ModpackException("不支持的MC版本: $mcVersion")
        }
        val loaderKey = index.dependencies.keys.firstOrNull { ModLoader.from(it) != null }
        if (loaderKey == null) {
            throw ModpackException("不支持的Mod加载器: 未知")
        }
        val parsedModloader = ModLoader.from(loaderKey)
        if (parsedModloader == null) {
            throw ModpackException("不支持的Mod加载器: $loaderKey")
        }
        val hashVersions = ModrinthApi.getVersionsFromHashes(index.files.map { it.hashes.sha1 })
        val projectIds = hashVersions.values.map { it.projectId }.distinct()
        val projects = ModrinthApi.getMultipleProjects(projectIds)
        val projectMap = projects.associateBy { it.id }
        val fileEntries = index.files.associateBy { it.hashes.sha1 }
        val mods = fileEntries.mapNotNull { (sha1, entry) ->
            val version = hashVersions[sha1] ?: return@mapNotNull null
            val project = projectMap[version.projectId]
            val slug = project?.slug?.takeIf { it.isNotBlank() }
                ?: entry.path.substringAfterLast('/').substringBeforeLast('.')
                    .ifBlank { version.projectId }
            val side = project?.run {
                if (serverSide == "unsupported") {
                    return@run Mod.Side.CLIENT
                }
                if (clientSide == "unsupported") {
                    return@run Mod.Side.SERVER
                }
                Mod.Side.BOTH
            } ?: Mod.Side.BOTH
            Mod(
                platform = "mr",
                projectId = version.projectId,
                slug = slug,
                fileId = version.id,
                hash = entry.hashes.sha1,
                side = side,
                downloadUrls = entry.downloads
            )
        }.fillModrinthVo()

        return Result.success(
            LoadedModpack(
                index = index,
                file = modpackFile,
                mods = mods,
                mcVersion = parsedMcVersion,
                modloader = parsedModloader
            )
        )
    }
    fun ModrinthProject.toCardVo(modFile: File? = null): Mod.CardVo {
        val briefInfo = slugBriefInfo[slug.trim().lowercase()]
        val icons = buildList {
            briefInfo?.logoUrl?.takeIf { it.isNotBlank() }?.let { add(it) }
            iconUrl?.takeIf { it.isNotBlank() }?.let { add(it) }
        }
        val resolvedName = (title ?: slug).ifBlank { slug }
        val iconBytes = modFile?.let {
            runCatching { JarFile(it).use { jar -> jar.modLogo } }.getOrNull()
        }
        val introText = description?.takeIf { it.isNotBlank() }?.trim()
            ?: modFile?.let { JarFile(it).readNeoForgeConfig()?.modDescription }
            ?: "暂无介绍"

    return Mod.CardVo(
        name = resolvedName,
        nameCn = briefInfo?.nameCn,
        intro = briefInfo?.intro ?: introText,
        iconData = iconBytes,
        iconUrls = icons,
        side = Mod.Side.BOTH
    )
}

    suspend fun List<Mod>.fillModrinthVo(): List<Mod> {
        val modsNeedingVo = filter { it.vo == null && it.platform == "mr" }
        if (modsNeedingVo.isEmpty()) return this

        val projectIds = modsNeedingVo.map { it.projectId }.distinct()
        val projects = ModrinthApi.getMultipleProjects(projectIds)
        val projectMap = projects.associateBy { it.id }

        forEach { mod ->
            if (mod.vo == null && mod.platform == "mr") {
                projectMap[mod.projectId]?.let { project ->
                    mod.vo = project.toCardVo(mod.file).copy(side = mod.side)
                }
            }
        }
        return this
    }
}
