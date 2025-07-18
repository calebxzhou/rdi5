package calebxzhou.rdi.model

import calebxzhou.rdi.model.RAccount
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class Room(
    @Contextual
    val _id: ObjectId = ObjectId(),
    val name: String,
    val score: Int=0,
    val members: List<Member> = arrayListOf(),

    ){
    @Volatile
    //临时id 最大0xff
    var tempId: Byte=0
    @Volatile
    var onlineMembers = hashMapOf<Byte, RAccount>()
    @Serializable
    data class Member(
        @Contextual
        val id: ObjectId,
        val isOwner: Boolean
    )

}