package calebxzhou.rdi.ihq.model

import calebxzhou.rdi.ihq.lgr
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId



@Serializable
data class Host(
    @Contextual
    val _id: ObjectId = ObjectId(),
    val name: String,
    val teamId: ObjectId,
    val modpack: Modpack,
    var port: Int,
) {

}