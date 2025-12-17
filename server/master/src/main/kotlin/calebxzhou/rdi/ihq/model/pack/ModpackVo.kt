package calebxzhou.rdi.ihq.model.pack

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class ModpackVo(
    @Contextual val id: ObjectId,
    val name:String,
    @Contextual val authorId: ObjectId,
    val authorName:String,
    val modCount: Int,
    val fileSize: Long,
    val icon: ByteArray?=null,
    val info: String="暂无简介",
) {
}