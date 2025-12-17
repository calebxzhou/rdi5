package calebxzhou.rdi.model.pack

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class ModpackDetailedVo(
    @Contextual val id: ObjectId,
    val name:String,
    @Contextual val authorId: ObjectId,
    val authorName:String,
    val modCount: Int,
    val icon: ByteArray?=null,
    val info: String="暂无简介",
    val modloader: String = "neoforge",
    val mcVer: String = "1.21.1",
    val versions: List<Modpack.Version> = arrayListOf()
) {
}