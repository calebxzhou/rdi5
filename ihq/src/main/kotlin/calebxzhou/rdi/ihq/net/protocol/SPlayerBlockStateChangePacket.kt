package calebxzhou.rdi.ihq.net.protocol

import calebxzhou.rdi.ihq.net.RByteBuf
import calebxzhou.rdi.ihq.net.SPacket

class SPlayerBlockStateChangePacket(
    val packedChunkPos: Int,// ChunkPos.asInt
    val sectionY: Byte,
    val sectionRelativeBlockPos: Short,
    val stateID: Int,
) : SPacket {
    constructor(buf: RByteBuf): this(buf.readInt(),buf.readByte(),buf.readShort(),buf.readInt())
    override fun handle() {
        TODO("Not yet implemented")
    }

}