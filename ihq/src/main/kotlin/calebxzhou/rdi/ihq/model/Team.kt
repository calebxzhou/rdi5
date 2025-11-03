package calebxzhou.rdi.ihq.model

import calebxzhou.rdi.ihq.model.Team.Role
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
    val members: List<Member> = arrayListOf(),
    val hostIds: List<@Contextual ObjectId> = emptyList(),
    val worldIds: List<@Contextual ObjectId> = emptyList(),
) {

    enum class Role(val level: Int) {
        OWNER(0),
        ADMIN(1),
        MEMBER(2),
        GUEST(10)
    }
    @Serializable
    data class Member(
        @Contextual
        val id: ObjectId,
        val role: Role
    )
}
