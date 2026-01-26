package calebxzhou.rdi.common.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.File

/**
 * calebxzhou @ 2026-01-25 22:34
 */
@Serializable
data class ModrinthModpackIndex(
    val formatVersion: Int,
    val game: String,
    val versionId: String,
    val name: String,
    val summary: String? = null,
    val files: List<FileEntry> = emptyList(),
    val dependencies: Map<String, String> = emptyMap()
) {
    @Serializable
    data class FileEntry(
        val path: String,
        val hashes: Hashes,
        val env: Env? = null,
        val downloads: List<String> = emptyList(),
        val fileSize: Long
    )

    @Serializable
    data class Hashes(
        val sha1: String,
        val sha512: String
    )

    @Serializable
    data class Env(
        val client: EnvSide,
        val server: EnvSide
    )

    @Serializable
    enum class EnvSide {
        @SerialName("required")
        required,
        @SerialName("optional")
        optional,
        @SerialName("unsupported")
        unsupported
    }
}
