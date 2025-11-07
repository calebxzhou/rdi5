package calebxzhou.rdi.model

import kotlinx.serialization.Serializable


@Serializable
data class CurseForgeFingerprintResponse(
    val data: CurseForgeFingerprintData? = null
)

@Serializable
data class CurseForgeFingerprintData(
    val isCacheBuilt: Boolean = false,
    val exactMatches: List<CurseForgeFingerprintMatch> = emptyList(),
    val exactFingerprints: List<Long> = emptyList(),
    val partialMatches: List<CurseForgeFingerprintMatch> = emptyList(),
    val partialMatchFingerprints: Map<String, List<Long>> = emptyMap(),
    val installedFingerprints: List<Long> = emptyList(),
    val unmatchedFingerprints: List<Long> = emptyList()
)

@Serializable
data class CurseForgeFingerprintMatch(
    val id: Long = 0,
    val file: CurseForgeFile,
    val latestFiles: List<CurseForgeFile> = emptyList()
)

@Serializable
data class CurseForgeFile(
    val id: Long = 0,
    val gameId: Long = 0,
    val modId: Long = 0,
    val isAvailable: Boolean = false,
    val displayName: String? = null,
    val fileName: String? = null,
    val releaseType: Int? = null,
    val fileStatus: Int? = null,
    val hashes: List<CurseForgeFileHash> = emptyList(),
    val fileDate: String? = null,
    val fileLength: Long? = null,
    val downloadCount: Long? = null,
    val fileSizeOnDisk: Long? = null,
    val downloadUrl: String? = null,
    val gameVersions: List<String> = emptyList(),
    val sortableGameVersions: List<CurseForgeSortableGameVersion> = emptyList(),
    val dependencies: List<CurseForgeDependency> = emptyList(),
    val exposeAsAlternative: Boolean? = null,
    val parentProjectFileId: Long? = null,
    val alternateFileId: Long? = null,
    val isServerPack: Boolean? = null,
    val serverPackFileId: Long? = null,
    val isEarlyAccessContent: Boolean? = null,
    val earlyAccessEndDate: String? = null,
    val fileFingerprint: Long? = null,
    val modules: List<CurseForgeModule> = emptyList()
)

@Serializable
data class CurseForgeFileHash(
    val value: String? = null,
    val algo: Int? = null
)

@Serializable
data class CurseForgeSortableGameVersion(
    val gameVersionName: String? = null,
    val gameVersionPadded: String? = null,
    val gameVersion: String? = null,
    val gameVersionReleaseDate: String? = null,
    val gameVersionTypeId: Int? = null
)

@Serializable
data class CurseForgeDependency(
    val modId: Long? = null,
    val relationType: Int? = null
)

@Serializable
data class CurseForgeModule(
    val name: String? = null,
    val fingerprint: Long? = null
)
