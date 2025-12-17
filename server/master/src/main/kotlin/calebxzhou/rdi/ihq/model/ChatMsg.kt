package calebxzhou.rdi.ihq.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

@Serializable
data class ChatMsg(
    @Contextual @BsonId val id: ObjectId = ObjectId(),
    @Contextual
    val senderId: ObjectId,
    val content: String,
){
    constructor(sender: RAccount, content: String,): this(ObjectId(),sender._id,content)
    fun toDto(sender: RAccount) = Dto(sender._id,sender.name,content)
    @Serializable
    data class Dto(
        @Contextual
        val senderId: ObjectId,
        val senderName: String,
        val content: String,
    )
}
