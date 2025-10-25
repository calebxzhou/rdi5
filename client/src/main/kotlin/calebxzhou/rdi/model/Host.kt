package calebxzhou.rdi.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId


@Serializable
data class Host(
    @Contextual
    val _id: ObjectId = ObjectId(),
    val name: String,
    @Contextual
    val teamId: ObjectId,
    @Contextual
    val modpackId: ObjectId,
    val packVer: String,
    @Contextual
    val worldId: ObjectId,
    var port: Int,
){
    companion object{
        var now : Host?=null
    }
}