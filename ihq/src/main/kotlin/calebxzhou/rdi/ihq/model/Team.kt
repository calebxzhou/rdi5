package calebxzhou.rdi.ihq.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class Team(
    @Contextual
    val _id: ObjectId = ObjectId(),
    val name: String,
    val info: String,
    val members: List<Member> = arrayListOf(),
) {
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