package calebxzhou.rdi.model

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
    val blockStates: List<RBlockState> = arrayListOf(),
    var firmSections: List<FirmSection> = arrayListOf()
){
    @Volatile
    //临时id 最大0xff
    var tempId: Byte=0
    @Volatile
    var onlineMembers = hashMapOf<Byte, RAccount.Dto>()
    @Serializable
    data class Member(
        @Contextual
        val id: ObjectId,
        val isOwner: Boolean
    )
    companion object{

        @JvmStatic
        var now: Room? = null
    }
}