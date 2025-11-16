package calebxzhou.rdi.service

import calebxzhou.rdi.Const
import calebxzhou.rdi.exception.ModpackException
import calebxzhou.rdi.lgr
import calebxzhou.rdi.model.*
import calebxzhou.rdi.model.pack.Mod
import calebxzhou.rdi.net.httpRequest
import calebxzhou.rdi.net.json
import calebxzhou.rdi.util.murmur2
import calebxzhou.rdi.util.serdesJson
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import java.io.File
import java.util.jar.JarFile
import java.util.zip.ZipFile
import kotlin.math.max

suspend fun List<File>.loadInfoCurseForge(): CurseForgeLocalResult {
    val hashToFile = this.associateBy { it.murmur2 }
    val hashes = hashToFile.keys.toList()
    val fingerprintData = CurseForgeService.matchFingerprintData(hashes)
    val result = CurseForgeService.getInfosFromHash(hashToFile, fingerprintData)
    //cache loaded info
    return result
}

object CurseForgeService {
    const val BASE_URL = "https://mod.mcimirror.top/curseforge/v1"//"https://api.curseforge.com/v1"
    suspend inline fun cfreq(path: String, method: HttpMethod = HttpMethod.Get, body: Any? = null) =
        httpRequest {
            url("${BASE_URL}/${path}")
            json()
            header("x-api-key", Const.CF_AKEY)
            body?.let { setBody(it) }
            this.method = method

        }

    //传入一堆murmur2格式的hash 返回cf匹配到的fingerprint data
    suspend fun matchFingerprintData(hashes: List<Long>): CurseForgeFingerprintData {
        @Serializable
        data class CurseForgeFingerprintRequest(val fingerprints: List<Long>)

        val response = cfreq(
            "fingerprints/432",
            HttpMethod.Post,
            CurseForgeFingerprintRequest(fingerprints = hashes)
        ).body<CurseForgeFingerprintResponse>()
        val data = response.data ?: CurseForgeFingerprintData()
        lgr.info(
            "CurseForge: ${data.exactMatches.size} exact matches, ${data.partialMatches.size} partial matches, ${data.unmatchedFingerprints.size} unmatched"
        )

        return data
    }

    suspend fun getFilesInfo(fileIds: List<Int>): List<CurseForgeFile> {
        @Serializable
        data class CFFileIdsRequest(val fileIds: List<Int>)
        @Serializable
        data class CFFilesResponse(val data: List<CurseForgeFile>)
        return cfreq(
            "mods/files",
            HttpMethod.Post,
            CFFileIdsRequest(fileIds)
        ).body<CFFilesResponse>().data

    }

    suspend fun getModFileInfo(modId: Int, fileId: Int): CurseForgeFile? {
        return cfreq("mods/${modId}/files/${fileId}").body<CurseForgeFileResponse>().data
    }

    //从mod project id列表获取cf mod信息
    suspend fun getModsInfo(modIds: List<Int>): List<CurseForgeModInfo> {
        @Serializable
        data class CurseForgeModsRequest(val modIds: List<Int>, val filterPcOnly: Boolean = true)
        @Serializable
        data class CurseForgeModsResponse(val data: List<CurseForgeModInfo>? = null)

        val mods = cfreq(
            "mods", HttpMethod.Post, CurseForgeModsRequest(modIds)
        ).body<CurseForgeModsResponse>().data!!.filter { it.isMod }
        lgr.info("CurseForge: fetched ${mods.size} mods for ${modIds.size} requested IDs")
        return mods
    }

    //从完整的cf mod信息取得card vo
    private fun CurseForgeModInfo.toBriefVo(modFile: File?=null): ModCardVo {
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

        return ModCardVo(
            name = resolvedName,
            nameCn = briefInfo?.nameCn,
            intro = briefInfo?.intro?:introText,
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
                    hash = record.fingerprint
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
        
        // Fill vo for mods that needed it
        return map { mod ->
            if (mod.vo == null && mod.platform == "cf") {
                mod.apply { vo = projectIdToVo[mod.projectId.toInt()] }
            } else {
                mod
            }
        }
    }
    
    suspend fun List<CurseForgePackManifest.File>.toMods(): List<Mod> {
        // Fetch all mod info and file info in parallel
        val modInfoMap = map { it.projectId }
            .distinct()
            .let { getModsInfo(it) }
            .associateBy { it.id }

        val fileInfoMap = map { it.fileId }
            .distinct()
            .let { getFilesInfo(it) }
            .associateBy { it.id }

        // Join the data by matching projectId and fileId
        return mapNotNull { curseFile ->
            val modInfo = modInfoMap[curseFile.projectId] ?: return@mapNotNull null
            val fileInfo = fileInfoMap[curseFile.fileId] ?: return@mapNotNull null

            Mod(
                platform = "cf",
                projectId = modInfo.id.toString(),
                slug = modInfo.slug,
                fileId = fileInfo.id.toString(),
                hash = fileInfo.fileFingerprint.toString()
            )
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

        val zip = try {
            ZipFile(zipFile)
        } catch (ex: Exception) {
            throw ModpackException("无法读取整合包: ${ex.message}")
        }

        try {
            val entries = zip.entries().asSequence().toList()
            entries.find { it.name == ".minecraft" }?.let { throw ModpackException("你应该选整合包 而不是客户端\n你可以用PCL的导出功能 将客户端转换为整合包") }
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

            if (manifest.minecraft.version != "1.21.1") {
                throw ModpackException("不支持的 MC 版本: ${manifest.minecraft.version}，当前只支持 1.21.1")
            }

            val loaderId = manifest.minecraft.modLoaders.firstOrNull { it.primary }?.id
                ?: manifest.minecraft.modLoaders.firstOrNull()?.id
            if (loaderId == null || !loaderId.startsWith("neoforge", ignoreCase = true)) {
                throw ModpackException("不支持的 Mod 加载器: ${loaderId ?: "未知"}，当前只支持 NeoForge")
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
    suspend fun Mod.getDownloadUrl(): String {
        return cfreq("${BASE_URL}/mods/${projectId}/files/${fileId}/download-url").body<String>()
    }
    private val MAX_PARALLEL_DOWNLOADS = max(4, Runtime.getRuntime().availableProcessors() * 2)

    fun handleCurseForgeDownloadUrls(url: String): List<String> {
        val variants = listOf(
            url.replace("-service.overwolf.wtf", ".forgecdn.net")
                .replace("://edge.", "://mediafilez.")
                .replace("://media.", "://mediafilez."),
            url.replace("://edge.", "://mediafilez.")
                .replace("://media.", "://mediafilez."),
            url.replace("-service.overwolf.wtf", ".forgecdn.net"),
            url.replace("://media.", "://edge."),
            url
        )
        return variants.distinct()
    }


    suspend fun downloadMod(mod: Mod) {
        //todo
    }

    suspend fun downloadMods(mods: List<Mod>) {
         //todo
    }

}

