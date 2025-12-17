package calebxzhou.rdi.model

import kotlinx.serialization.Serializable

@Serializable
data class ModrinthVersionLookupRequest(
    val hashes: List<String>,
    val algorithm: String,
)

