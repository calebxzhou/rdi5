package calebxzhou.rdi.ihq.model

import kotlinx.serialization.Serializable
import net.benwoodworth.knbt.NbtCompound

@Serializable
data class RBlockEntity(
    //位置（0~4096）
    val pos: Short,
    val data: NbtCompound
)
