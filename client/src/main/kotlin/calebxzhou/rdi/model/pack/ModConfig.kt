package calebxzhou.rdi.model.pack

import kotlinx.serialization.Serializable

@Serializable
data class ModConfig(
    val path: String,
    val content: String,
) {

}