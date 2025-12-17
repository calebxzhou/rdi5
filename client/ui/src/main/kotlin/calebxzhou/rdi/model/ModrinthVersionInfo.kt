package calebxzhou.rdi.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModrinthVersionInfo(
    val id: String,
    val name: String,
    @SerialName("version_number") val versionNumber: String,
    @SerialName("project_id") val projectId: String,
    @SerialName("author_id") val authorId: String? = null,
    val changelog: String? = null,
    @SerialName("changelog_url") val changelogUrl: String? = null,
    @SerialName("version_type") val versionType: String? = null,
    val status: String? = null,
    @SerialName("requested_status") val requestedStatus: String? = null,
    @SerialName("game_versions") val gameVersions: List<String> = emptyList(),
    val loaders: List<String> = emptyList(),
    val featured: Boolean? = null,
    val downloads: Long? = null,
    @SerialName("date_published") val datePublished: String? = null,
    val files: List<ModrinthFileInfo> = emptyList(),
    val dependencies: List<ModrinthDependency> = emptyList()
)
