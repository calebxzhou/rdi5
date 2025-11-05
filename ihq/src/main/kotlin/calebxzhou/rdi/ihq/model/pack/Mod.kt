package calebxzhou.rdi.ihq.model.pack

import kotlinx.serialization.Serializable

@Serializable
data class Mod(
    val platform: String,//cf / mr
    val projectId: String,
    val slug: String,
    val fileId: String,
    val hash: String,
) {
}