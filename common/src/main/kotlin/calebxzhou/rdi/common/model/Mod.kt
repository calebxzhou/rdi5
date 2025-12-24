package calebxzhou.rdi.common.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.File

@Serializable
data class Mod(
    val platform: String,//cf / mr
    val projectId: String,
    val slug: String,
    val fileId: String,
    val hash: String,
    val side: Side= Side.BOTH,
) {
    val fileName = "${slug}_${platform}_${hash}.jar"
    enum class Side{
        CLIENT,SERVER,BOTH
    }
    @Transient
    var vo: ModCardVo?=null
    @Transient
    var file: File?=null
}