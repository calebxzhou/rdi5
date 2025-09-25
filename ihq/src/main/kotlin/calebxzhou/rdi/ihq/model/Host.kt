package calebxzhou.rdi.ihq.model

import calebxzhou.rdi.ihq.model.pack.Modpack
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId



@Serializable
data class Host(
    @Contextual
    val _id: ObjectId = ObjectId(),
    val name: String,
    val teamId: ObjectId,
    val modpackId: ObjectId,
    val packVer: String,
    val worldId: ObjectId,
    var port: Int,
) {

}