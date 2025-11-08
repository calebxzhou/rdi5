package calebxzhou.rdi.model.pack

import calebxzhou.rdi.model.ModCardVo
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
) {
    @Transient
    var vo: ModCardVo?=null
    @Transient
    var file: File?=null
}