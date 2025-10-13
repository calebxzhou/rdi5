package calebxzhou.rdi.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class ModrinthDependency(
    @SerialName("version_id") val versionId: String? = null,
    @SerialName("project_id") val projectId: String? = null,
    @SerialName("dependency_type") val dependencyType: String? = null
)