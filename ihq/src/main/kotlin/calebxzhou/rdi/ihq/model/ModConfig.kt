package calebxzhou.rdi.ihq.model

import kotlinx.serialization.Serializable

@Serializable
data class ModConfig(
    val path: String,
    val content: String,
) {

}