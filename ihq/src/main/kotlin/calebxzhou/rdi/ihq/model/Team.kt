package calebxzhou.rdi.ihq.model

import calebxzhou.rdi.ihq.lgr
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class Team(
    @Contextual
    val _id: ObjectId = ObjectId(),
    val name: String,
    val info: String,
    val score: Int=0,
    val hostIds: List<ObjectId> = arrayListOf(),
    val worldIds: List<ObjectId> = arrayListOf(),
    val members: List<Member> = arrayListOf(),
) {
    fun hasMember(id: ObjectId): Boolean {
        return members.any { it.id == id }
    }
    val owner
        get() =  members.find { it.role== Role.OWNER } ?: let{
            lgr.error { "找不到房间$_id 的岛主" }
            Member(ObjectId(), Role.GUEST)
        }
    enum class Role {
        OWNER,
        ADMIN,
        MEMBER,
        GUEST
    }
    @Serializable
    data class Member(
        @Contextual
        val id: ObjectId,
        val role: Role
    )
}