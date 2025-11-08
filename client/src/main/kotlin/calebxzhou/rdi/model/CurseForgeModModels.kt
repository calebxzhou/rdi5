package calebxzhou.rdi.model

import calebxzhou.rdi.model.pack.Mod
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class CurseForgeModsRequest(
    val modIds: List<Long>,
    val filterPcOnly: Boolean = true
)

@Serializable
data class CurseForgeModsResponse(
    val data: List<CurseForgeMod>? = null
)

@Serializable
data class CurseForgeMod(
    val id: Long? = null,
    val gameId: Long? = null,
    val name: String? = null,
    val slug: String,
    val links: CurseForgeModLinks? = null,
    val summary: String? = null,
    val status: Int? = null,
    val downloadCount: Long? = null,
    val isFeatured: Boolean? = null,
    val primaryCategoryId: Long? = null,
    val categories: List<CurseForgeCategoryInfo> = emptyList(),
    val classId: Long? = null,
    val authors: List<CurseForgeAuthor> = emptyList(),
    val logo: CurseForgeAsset? = null,
    val screenshots: List<CurseForgeAsset> = emptyList(),
    val mainFileId: Long? = null,
    val latestFiles: List<CurseForgeFile> = emptyList(),
    val latestFilesIndexes: List<CurseForgeFileIndex> = emptyList(),
    val latestEarlyAccessFilesIndexes: List<CurseForgeFileIndex> = emptyList(),
    val dateCreated: String? = null,
    val dateModified: String? = null,
    val dateReleased: String? = null,
    val allowModDistribution: Boolean? = null,
    val gamePopularityRank: Long? = null,
    val isAvailable: Boolean? = null,
    val thumbsUpCount: Long? = null,
    val rating: Double? = null
)

@Serializable
data class CurseForgeModLinks(
    val websiteUrl: String? = null,
    val wikiUrl: String? = null,
    val issuesUrl: String? = null,
    val sourceUrl: String? = null
)

@Serializable
data class CurseForgeCategoryInfo(
    val id: Long? = null,
    val gameId: Long? = null,
    val name: String? = null,
    val slug: String? = null,
    val url: String? = null,
    val iconUrl: String? = null,
    val dateModified: String? = null,
    val isClass: Boolean? = null,
    val classId: Long? = null,
    val parentCategoryId: Long? = null,
    val displayIndex: Long? = null
)

@Serializable
data class CurseForgeAuthor(
    val id: Long? = null,
    val name: String? = null,
    val url: String? = null
)

@Serializable
data class CurseForgeAsset(
    val id: Long? = null,
    val modId: Long? = null,
    val title: String? = null,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val url: String? = null
)

@Serializable
data class CurseForgeFileIndex(
    val gameVersion: String? = null,
    val fileId: Long? = null,
    val filename: String? = null,
    val releaseType: Int? = null,
    val gameVersionTypeId: Long? = null,
    val modLoader: Int? = null
)


data class CurseForgeLocalResult(
    val matched: List<Mod> = emptyList(),
    val unmatched: List<File> = emptyList()
)