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
    val centerPos: ByteArray = ByteArray(3),
    val members: List<Member> = arrayListOf(),


    ) {
    //临时id 最大0xff
    @Volatile
    var tempId: Byte=0
    @Volatile
    var onlineMembers = hashMapOf<Byte,RAccount>()

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Room

        if (score != other.score) return false
        if (tempId != other.tempId) return false
        if (_id != other._id) return false
        if (name != other.name) return false
        if (containerId != other.containerId) return false
        if (!centerPos.contentEquals(other.centerPos)) return false
        if (members != other.members) return false
        if (onlineMembers != other.onlineMembers) return false
        if (owner != other.owner) return false

        return true
    }

    override fun hashCode(): Int {
        var result = score
        result = 31 * result + tempId
        result = 31 * result + _id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + containerId.hashCode()
        result = 31 * result + centerPos.contentHashCode()
        result = 31 * result + members.hashCode()
        result = 31 * result + onlineMembers.hashCode()
        result = 31 * result + owner.hashCode()
        return result
    }


}