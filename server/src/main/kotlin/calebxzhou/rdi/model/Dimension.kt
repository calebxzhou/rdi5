package calebxzhou.rdi.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class Dimension(
    val id: String,
    //持久子区块id
    @Contextual
    val firmSections: List<@Contextual ObjectId>
) {
}