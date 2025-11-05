package calebxzhou.rdi.ihq.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class World(
    @Contextual val _id: ObjectId = ObjectId(),
    val name: String,
    //所属团队
    @Contextual
    val teamId: ObjectId,
    //如果mount不同的modpack  警告用户可能坏档
    @Contextual
    val packId: ObjectId,

    var size : Long = 0,
){

}
