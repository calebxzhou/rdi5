package calebxzhou.rdi.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class ModrinthFileInfo(
    val filename: String,
    val url: String,
    val primary: Boolean = false,
    val size: Long? = null,
    val hashes: Map<String, String> = emptyMap(),
    @SerialName("file_type") val fileType: String? = null
)
