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
    @Contextual
    val teamId: ObjectId,
    @Contextual
    val modpackId: ObjectId,
    val packVer: String,
    @Contextual
    val worldId: ObjectId,
    var port: Int,
) {

}

fun Host.imageRef(): String = "${modpackId.toHexString()}:$packVer"