package calebxzhou.rdi.service

import calebxzhou.rdi.Const
import calebxzhou.rdi.lgr
import calebxzhou.rdi.model.*
import calebxzhou.rdi.model.pack.Mod
import calebxzhou.rdi.net.httpRequest
import calebxzhou.rdi.net.json
import calebxzhou.rdi.util.murmur2
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import java.io.File
import java.util.jar.JarFile

suspend fun List<File>.loadInfoCurseForge(): CurseForgeLocalResult {
    val hashToFile = this.associateBy { it.murmur2 }
    val hashes = hashToFile.keys.toList()
    val fingerprintData = CurseForgeService.matchFingerprintData(hashes)
    val result = CurseForgeService.getInfosFromHash(hashToFile, fingerprintData)
    //cache loaded info
    return result
}

object CurseForgeService {
    const val BASE_URL = "https://api.curseforge.com/v1"

    //传入一堆murmur2格式的hash 返回cf匹配到的fingerprint data
    suspend fun matchFingerprintData(hashes: List<Long>): CurseForgeFingerprintData {
        @Serializable data class CurseForgeFingerprintRequest(val fingerprints: List<Long>)

        val response = httpRequest {
            url("${BASE_URL}/fingerprints/432")
            method = HttpMethod.Post
            json()
            setBody(CurseForgeFingerprintRequest(fingerprints = hashes))
            header("x-api-key", Const.CF_AKEY)
        }.body<CurseForgeFingerprintResponse>()
        val data = response.data ?: CurseForgeFingerprintData()
        lgr.info(
            "CurseForge: ${data.exactMatches.size} exact matches, ${data.partialMatches.size} partial matches, ${data.unmatchedFingerprints.size} unmatched"
        )

        return data
    }
    //从mod project id列表获取cf mod信息
    suspend fun getInfos(modIds: List<Long>): List<CurseForgeMod> {
        val mods = httpRequest {
            method = HttpMethod.Post
            json()
            url("${BASE_URL}/mods")
            header("x-api-key", Const.CF_AKEY)
            setBody(
                CurseForgeModsRequest(
                    modIds = modIds,
                    filterPcOnly = true
                )
            )
        }.body<CurseForgeModsResponse>().data!!
        lgr.info("CurseForge: fetched ${mods.size} mods for ${modIds.size} requested IDs")
        return mods
    }

    //从完整的cf mod信息取得card vo
    private fun CurseForgeMod.toBriefVo(modFile: File?): ModCardVo {
        val icons = buildList {
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
            nameCn = null,
            intro = introText,
            iconData = iconBytes,
            iconUrls = icons
        )
    }


    suspend fun getInfosFromHash(
        hashToFile: Map<Long, File>,
        fingerprintData: CurseForgeFingerprintData
    ): CurseForgeLocalResult {

        data class MatchRecord(val projectId: Long, val fileId: String, val fingerprint: String, val file: File)

        val matchRecords = fingerprintData.exactMatches.mapNotNull { match ->
            val projectId = match.id.takeIf { it > 0 } ?: return@mapNotNull null
            val fingerprint = match.file.fileFingerprint
                ?: match.latestFiles.firstOrNull()?.fileFingerprint
                ?: return@mapNotNull null
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

        val cfMods = getInfos(modIds)

        data class CfModMeta(
            val projectId: Long,
            val canonicalSlug: String,
            val normalizedSlug: String,
            val files: List<MatchRecord>,
            val mod: CurseForgeMod
        )

        val cfModMeta = cfMods.mapNotNull { mod ->
            val projectId = mod.id ?: return@mapNotNull null
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

        val discoveredMods = cfModMeta.flatMap { meta ->
            meta.files.map { record ->
                Mod(
                    platform = "cf",
                    projectId = meta.projectId.toString(),
                    slug = meta.canonicalSlug,
                    fileId = record.fileId,
                    hash = record.fingerprint
                ).apply { vo = slugBriefInfo[meta.normalizedSlug]?.toVo(record.file)
                    ?: meta.mod.toBriefVo(record.file)  }
            }
        }
        val unmatched = hashToFile.values.filterNot { it in matchedFiles }
        lgr.info("curseforge没找到这些mod：${unmatched}")
        return CurseForgeLocalResult( matchedFiles, unmatched,discoveredMods)

    }
}