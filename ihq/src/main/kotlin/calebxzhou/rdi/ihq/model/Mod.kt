package calebxzhou.rdi.ihq.model

import kotlinx.serialization.Serializable

@Serializable
data class Mod(
    val id: String,
    val platform: String, // e.g., "curseforge", "modrinth"
    val comment: String,
    val version: String,
) {
}