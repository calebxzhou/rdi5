package calebxzhou.rdi.ihq.model.pack

import kotlinx.serialization.Serializable

@Serializable
data class Mod(
    val platform: String,//cf / mr / rdi
    val projectId: String,
    val slug: String,
    val fileId: String,
    val hash: String,
    val side: Side = Side.BOTH,
) {
    val fileName = "${slug}_${platform}_${hash}.jar"
    enum class Side{
        CLIENT,SERVER,BOTH
    }
}