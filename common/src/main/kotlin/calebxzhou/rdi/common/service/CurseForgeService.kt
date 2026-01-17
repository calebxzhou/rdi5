package calebxzhou.rdi.common.service

import calebxzhou.mykotutils.curseforge.CurseForgeApi
import calebxzhou.mykotutils.curseforge.CurseForgeApi.getModsInfo
import calebxzhou.mykotutils.curseforge.CurseForgeFingerprintData
import calebxzhou.mykotutils.curseforge.CurseForgeModInfo
import calebxzhou.mykotutils.log.Loggers
import calebxzhou.mykotutils.modrinth.ModrinthApi.mapModrinthProjects
import calebxzhou.mykotutils.std.murmur2
import calebxzhou.mykotutils.std.openChineseZip
import calebxzhou.rdi.common.exception.ModpackException
import calebxzhou.rdi.common.model.*
import calebxzhou.rdi.common.serdesJson
import calebxzhou.rdi.common.service.ModService.briefInfo
import calebxzhou.rdi.common.service.ModService.modDescription
import calebxzhou.rdi.common.service.ModService.modLogo
import calebxzhou.rdi.common.service.ModService.readNeoForgeConfig
import calebxzhou.rdi.common.service.ModService.toVo
import calebxzhou.rdi.service.ModrinthService.mr2CfSlug
import java.io.File
import java.util.jar.JarFile


object CurseForgeService {
    val slugBriefInfo: Map<String, ModBriefInfo> by lazy { ModService.buildSlugMap(briefInfo) { it.curseforgeSlugs } }
    private val lgr by Loggers

    //镜像源可能会缺mod  比如McJtyLib - 1.21-9.0.14
    const val MIRROR_URL = "https://mod.mcimirror.top/curseforge/v1"
    const val OFFICIAL_URL = "https://api.curseforge.com/v1"



    suspend fun List<File>.loadInfoCurseForge(): CurseForgeLocalResult {
        val hashToFile = this.associateBy { it.murmur2 }
        val hashes = hashToFile.keys.toList()
        val fingerprintData = CurseForgeApi.matchFingerprintData(hashes)
        val result = getInfosFromHash(hashToFile, fingerprintData)
        //cache loaded info
        return result
    }


    //从完整的cf mod信息取得card vo
    private fun CurseForgeModInfo.toBriefVo(modFile: File? = null): Mod.CardVo {
        val briefInfo = slugBriefInfo[slug]
        val icons = buildList {
            briefInfo?.logoUrl?.let { add(it) }
            logo?.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { add(it) }
            logo?.url?.takeIf { it.isNotBlank() }?.let { add(it) }
        }
        val resolvedName = (name ?: slug).ifBlank { slug }
        val iconBytes = modFile?.let {
            runCatching { JarFile(it).use { jar -> jar.modLogo } }.getOrNull()
        }
        val introText = summary?.takeIf { it.isNotBlank() }?.trim()
            ?: modFile?.let { JarFile(it).readNeoForgeConfig()?.modDescription }
            ?: "暂无介绍"

        return Mod.CardVo(
            name = resolvedName,
            nameCn = briefInfo?.nameCn,
            intro = briefInfo?.intro ?: introText,
            iconData = iconBytes,
            iconUrls = icons
        )
    }


    suspend fun getInfosFromHash(
        hashToFile: Map<Long, File>,
        fingerprintData: CurseForgeFingerprintData
    ): CurseForgeLocalResult {

        data class MatchRecord(val projectId: Int, val fileId: String, val fingerprint: String, val file: File)

        val matchRecords = fingerprintData.exactMatches.mapNotNull { match ->
            val projectId = match.id.takeIf { it > 0 } ?: return@mapNotNull null
            val fingerprint = match.file.fileFingerprint
            val localFile = hashToFile[fingerprint] ?: return@mapNotNull null
            MatchRecord(
                projectId = projectId,
                fileId = match.file.id.toString(),
                fingerprint = fingerprint.toString(),
                file = localFile
            )
        }.groupBy { it.projectId }

        val modIds = matchRecords.keys.toList()
        if (modIds.isEmpty()) {
            lgr.info("mod id 是空的")
            return CurseForgeLocalResult()
        }

        val cfMods = getModsInfo(modIds)

        data class CfModMeta(
            val projectId: Int,
            val canonicalSlug: String,
            val normalizedSlug: String,
            val files: List<MatchRecord>,
            val mod: CurseForgeModInfo
        )

        val cfModMeta = cfMods.mapNotNull { mod ->
            val projectId = mod.id
            val files = matchRecords[projectId].orEmpty()
            if (files.isEmpty()) return@mapNotNull null
            val rawSlug = mod.slug.trim()
            val canonicalSlug = when {
                rawSlug.isNotEmpty() -> rawSlug
                !mod.name.isNullOrBlank() -> mod.name.trim()
                else -> projectId.toString()
            }
            CfModMeta(
                projectId = projectId,
                canonicalSlug = canonicalSlug,
                normalizedSlug = canonicalSlug.lowercase(),
                files = files,
                mod = mod
            )
        }

        val matchedFiles = cfModMeta.flatMap { meta -> meta.files.map { it.file } }.toList()

        val matched = cfModMeta.flatMap { meta ->
            meta.files.map { record ->
                Mod(
                    platform = "cf",
                    projectId = meta.projectId.toString(),
                    slug = meta.canonicalSlug,
                    fileId = record.fileId,
                    hash = record.fingerprint,

                    ).apply {
                    file = record.file
                    vo = slugBriefInfo[meta.normalizedSlug]?.toVo(record.file)
                        ?: meta.mod.toBriefVo(record.file)
                }
            }
        }
        val unmatched = hashToFile.values.filterNot { it in matchedFiles }
        lgr.info("curseforge没找到这些mod：${unmatched}")
        return CurseForgeLocalResult(matched, unmatched)

    }

    /**
     * Fill ModCardVo for CurseForge mods that don't have vo yet
     * @return List of mods with vo filled
     */
    suspend fun List<Mod>.fillCurseForgeVo(): List<Mod> {
        // Filter mods that need vo and are from CurseForge
        val modsNeedingVo = filter { it.vo == null && it.platform == "cf" }
        if (modsNeedingVo.isEmpty()) return this

        // Batch fetch mod info for all mods needing vo
        val projectIdToMod = modsNeedingVo.associateBy { it.projectId.toInt() }
        val modInfos = getModsInfo(projectIdToMod.keys.toList())
        val projectIdToVo = modInfos.associate { it.id to it.toBriefVo() }
        forEach { mod ->
            if (mod.vo == null && mod.platform == "cf") {
                mod.apply { vo = projectIdToVo[mod.projectId.toInt()] }
            }
        }
        return this
    }

    suspend fun List<CurseForgePackManifest.File>.mapMods(): List<Mod> {

        // Fetch all mod info and file info in parallel
        val modInfoMap = getModsInfo(map{it.projectId})
            .associateBy { it.id }
        //从modrinth读info是为了知道client side/ server side
        val cfSlugMrInfo = modInfoMap.values.map { cfInfo ->
            cfInfo.slug.cf2MrSlug
        }.mapModrinthProjects().associateBy { it.slug.mr2CfSlug }

        val fileInfoMap = CurseForgeApi.getModFilesInfo(map { it.fileId })
            .associateBy { it.id }
        val libraryModSlugs = arrayListOf<String>()
        // Join the data by matching projectId and fileId
        return mapNotNull { curseFile ->
            val modInfo = modInfoMap[curseFile.projectId] ?: let {
                lgr.error("mod ${curseFile.projectId}/${curseFile.fileId} 在mod info map没有信息")
                return@mapNotNull null
            }
            val cfSlug = modInfo.slug
            //不需要kff client/server已经有了
            if(cfSlug == "kotlin-for-forge") return@mapNotNull null
            //todo 删掉rgp client 删掉fancymenu及其dependents
            val fileInfo = fileInfoMap[curseFile.fileId] ?: let {
                lgr.error("mod ${curseFile.projectId}/${curseFile.fileId} file info map没有信息")
                return@mapNotNull null
            }
            val side = cfSlugMrInfo[cfSlug]?.run {
                if(categories.contains("library")) {
                    libraryModSlugs+=cfSlug
                    return@run Mod.Side.BOTH
                }
                if (serverSide == "unsupported") {
                    return@run Mod.Side.CLIENT
                }
                if (clientSide == "unsupported") {
                    return@run Mod.Side.SERVER
                } else return@run Mod.Side.BOTH
            } ?: Mod.Side.BOTH
            Mod(
                platform = "cf",
                projectId = modInfo.id.toString(),
                slug = cfSlug,
                fileId = fileInfo.id.toString(),
                hash = fileInfo.fileFingerprint.toString(),
                side = side
            ).apply {
                vo = modInfo.toBriefVo()
            }
        }.also { mod ->

            lgr.info("lib mod: ${libraryModSlugs.joinToString(",")}")
            lgr.info("server mod：${mod.filter { it.side == Mod.Side.SERVER }.map { it.slug }}")
            lgr.info("client mod：${mod.filter { it.side == Mod.Side.CLIENT }.map { it.slug }}")
            lgr.info("both  mod：${mod.filter { it.side == Mod.Side.BOTH }.map { it.slug }}")
        }
    }

    /**
     * Load and validate a CurseForge modpack from a ZIP file
     * @param zipPath Path to the modpack ZIP file
     * @return Parsed manifest and list of override entries with their root prefix
     * @throws ModpackException if validation fails
     */
    suspend fun loadModpack(zipPath: String): CurseForgeModpackData {
        if (zipPath.isBlank()) {
            throw ModpackException("请先输入整合包路径")
        }

        val zipFile = File(zipPath)
        if (!zipFile.exists() || !zipFile.isFile) {
            throw ModpackException("找不到整合包文件: ${zipFile.path}")
        }

        val zip = zipFile.openChineseZip()

        try {
            val entries = zip.entries().asSequence().toList()
            entries.find { it.name == ".minecraft" }
                ?.let { throw ModpackException("你应该选整合包 而不是客户端\n你可以用PCL的导出功能 将客户端转换为整合包") }
            //玩不了gto
            val hasGtoCore = entries.any { it.name.startsWith("overrides/mods/gtocore") }
            val hasGtoNativeLib = entries.any { it.name.startsWith("overrides/mods/gtonativelib") }
            if (hasGtoCore && hasGtoNativeLib) {
                throw ModpackException("rdi核心被这个包强制禁用了 玩不了 请换个包")
            }
            val manifestEntry = entries.firstOrNull {
                !it.isDirectory && it.name.substringAfterLast('/') == "manifest.json"
            } ?: throw ModpackException("整合包缺少文件：manifest.json")

            val rootPrefix = manifestEntry.name.substringBeforeLast('/', missingDelimiterValue = "")
                .let { if (it.isBlank()) "" else "$it/" }
            val overridesFolder = rootPrefix + "overrides/"
            val overrideEntries = entries.filter { !it.isDirectory && it.name.startsWith(overridesFolder) }

            if (overrideEntries.isEmpty()) {
                throw ModpackException("整合包缺少目录：overrides")
            }

            val manifestJson = zip.getInputStream(manifestEntry).bufferedReader(Charsets.UTF_8).use { it.readText() }
            val manifest = runCatching {
                serdesJson.decodeFromString<CurseForgePackManifest>(manifestJson)
            }.getOrElse {
                throw ModpackException("manifest.json 解析失败: ${it.message}")
            }

            val supportedVersion = McVersion.from(manifest.minecraft.version)
            if (supportedVersion == null) {
                val supportedList = McVersion.entries.joinToString(", ") { it.mcVer }
                throw ModpackException("不支持的 MC 版本: ${manifest.minecraft.version}，当前只支持: $supportedList")
            }

            val loaderId = manifest.minecraft.modLoaders.firstOrNull { it.primary }?.id
                ?: manifest.minecraft.modLoaders.firstOrNull()?.id
            val loaderSupported = loaderId?.startsWith("neoforge", ignoreCase = true) == true ||
                loaderId?.startsWith("forge", ignoreCase = true) == true
            if (!loaderSupported) {
                throw ModpackException("不支持的 Mod 加载器: ${loaderId ?: "未知"}，当前只支持 Forge/NeoForge")
            }

            val modpackName = manifest.name.trim()
            if (modpackName.isEmpty()) {
                throw ModpackException("manifest.json 中缺少整合包名称")
            }

            val versionName = manifest.version.trim()
            if (versionName.isEmpty()) {
                throw ModpackException("manifest.json 中缺少版本号")
            }

            return CurseForgeModpackData(
                manifest = manifest,
                zipFile,
                zip = zip,
                overrideEntries = overrideEntries,
                overridesFolder = overridesFolder
            )
        } catch (e: ModpackException) {
            zip.close()
            throw e
        } catch (e: Exception) {
            zip.close()
            throw ModpackException("处理整合包时出错: ${e.message}")
        }
    }


    //cf - mr
    val String.cf2MrSlug: String
        get() {
            if (isBlank()) return this
            val info = slugBriefInfo[trim().lowercase()] ?: return this
            return info.modrinthSlugs.firstOrNull { it.isNotBlank() }?.trim() ?: this
        }


}

