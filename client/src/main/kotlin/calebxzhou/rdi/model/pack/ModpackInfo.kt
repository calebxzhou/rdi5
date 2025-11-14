package calebxzhou.rdi.model.pack

import kotlinx.serialization.Serializable

@Serializable
data class ModpackInfo(
    val name:String,
    val author: String = "",
    val modCount: Int,
    val fileSize: Long,
    val icon: ByteArray?=null,
    val info: String="暂无简介",
) {
}