package calebxzhou.rdi.ihq.model

import kotlinx.serialization.Serializable
import net.benwoodworth.knbt.NbtCompound

@Serializable
data class RBlockEntity(
    val inPos: Short,
    val data: NbtCompound
)
