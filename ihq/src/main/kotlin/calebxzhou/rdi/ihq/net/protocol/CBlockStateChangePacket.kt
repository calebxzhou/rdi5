package calebxzhou.rdi.ihq.net.protocol

import calebxzhou.rdi.ihq.net.CPacket
import calebxzhou.rdi.ihq.net.RByteBuf

class CBlockStateChangePacket(
    val packedChunkPos: Int,// ChunkPos.asInt
    val sectionY: Byte,
    val sectionRelativeBlockPos: Short,
    val stateID: Int,
) : CPacket {

    override fun write(buf: RByteBuf) {
        buf.writeInt(packedChunkPos)
            .writeByte(sectionY.toInt())
            .writeShort(sectionRelativeBlockPos.toInt())
            .writeInt(stateID)
    }
}