package calebxzhou.rdi.model.pack

import kotlinx.serialization.Serializable

@Serializable
data class Mod(
    val platform: String,//cf / mr
    val projectId: String,
    val fileId: String,
) {
}