package calebxzhou.rdi.model

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
