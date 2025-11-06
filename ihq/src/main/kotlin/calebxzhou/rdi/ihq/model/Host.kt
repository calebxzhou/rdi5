package calebxzhou.rdi.ihq.model

import calebxzhou.rdi.ihq.model.pack.Mod
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
    //版本可能会重新发布 此时id会变 所以不用packVerId
    val packVer: String = "latest",
    @Contextual
    val worldId: ObjectId,
    var port: Int,
    //整合包外的附加mod
    val extraMods: List<Mod> = arrayListOf()
) {

}

fun Host.imageRef(): String = "${modpackId}:${this.packVer}"