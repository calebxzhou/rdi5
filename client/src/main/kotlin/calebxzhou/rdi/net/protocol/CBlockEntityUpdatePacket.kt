package calebxzhou.rdi.net.protocol

import calebxzhou.rdi.net.CPacket
import calebxzhou.rdi.net.RByteBuf
import net.minecraft.nbt.CompoundTag

/**
 * 更新玩家操作的方块实体数据（）
 */
data class CBlockEntityUpdatePacket(
    val packedChunkPos: Int,// ChunkPos.asInt
    val sectionY: Byte,
    val sectionRelativeBlockPos: Short,
    val data: CompoundTag
): CPacket {
    constructor(buf: RByteBuf): this(
        packedChunkPos = buf.readInt(),
        sectionY = buf.readByte(),
        sectionRelativeBlockPos = buf.readShort(),
        data = buf.readNbt() as CompoundTag
    )
    override fun handle() {
        TODO("Not yet implemented")
    }
}