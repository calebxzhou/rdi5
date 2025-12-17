package calebxzhou.rdi.ihq.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import net.benwoodworth.knbt.NbtCompound
import org.bson.types.ObjectId

data class RPlayerData(
    val _id: ObjectId = ObjectId(),
    val data: NbtCompound
)
