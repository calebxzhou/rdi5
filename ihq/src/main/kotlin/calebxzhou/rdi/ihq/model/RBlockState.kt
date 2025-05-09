package calebxzhou.rdi.ihq.model

import kotlinx.serialization.Serializable
import net.benwoodworth.knbt.Nbt
import net.benwoodworth.knbt.NbtCompound
import org.bson.BSONObject

@Serializable
data class RBlockState(
    val name: String,
    val props: Map<String, String> = emptyMap()
){

}