package calebxzhou.rdi.ihq.net.protocol

import calebxzhou.rdi.ihq.net.CPacket
import calebxzhou.rdi.ihq.net.RByteBuf
import calebxzhou.rdi.ihq.net.writeNbt
import net.benwoodworth.knbt.NbtCompound

/**
 * 更新玩家操作的方块实体数据（）
 */
data class CBlockEntityUpdatePacket(
    val packedChunkPos: Int,// ChunkPos.asInt
    val sectionY: Byte,
    val sectionRelativeBlockPos: Short,
    val data: NbtCompound
): CPacket {
    override fun write(buf: RByteBuf) {
        buf.writeInt(packedChunkPos)
            .writeByte(sectionY.toInt())
            .writeShort(sectionRelativeBlockPos.toInt())
            .writeNbt(data)
    }
}