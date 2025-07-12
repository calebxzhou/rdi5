package calebxzhou.rdi.net.protocol

import calebxzhou.rdi.net.CPacket
import calebxzhou.rdi.net.RByteBuf

class CBlockStateChangePacket(
    val packedChunkPos: Int,// ChunkPos.asInt
    val sectionY: Byte,
    val sectionRelativeBlockPos: Short,
    val stateID: Int,
) : CPacket {
    constructor(buf: RByteBuf): this(
        packedChunkPos = buf.readInt(),
        sectionY = buf.readByte(),
        sectionRelativeBlockPos = buf.readShort(),
        stateID = buf.readInt()
    )

    override fun handle() {
        TODO("Not yet implemented")
    }
}