package calebxzhou.rdi.ihq.model

import calebxzhou.rdi.ihq.lgr
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class Room(
    @Contextual
    val _id: ObjectId = ObjectId(),
    val name: String,
    val containerId: String,
    val score: Int=0,
    val centerPos: IslandBlockPos = IslandBlockPos(),
    val members: List<Member> = arrayListOf(),
) {
    var port = 0
    fun hasMember(pid: ObjectId): Boolean {
        return members.find { it.id==pid } != null
    }
    val owner
        get() = members.find { it.isOwner } ?: let{
            lgr.error { "找不到房间$_id 的岛主" }
            Member(ObjectId(),false)
        }
    @Serializable
    data class Member(
        @Contextual
        val id: ObjectId,
        val isOwner: Boolean
    )




}