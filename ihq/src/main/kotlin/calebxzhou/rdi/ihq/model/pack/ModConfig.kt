package calebxzhou.rdi.ihq.model.pack

import kotlinx.serialization.Serializable

@Serializable
data class ModConfig(
    val path: String,
    val content: String,
) {

}