package calebxzhou.rdi.ihq.net.protocol

import calebxzhou.rdi.ihq.net.RByteBuf
import calebxzhou.rdi.ihq.net.SPacket
import io.netty.channel.ChannelHandlerContext

class CBlockStateChangePacket(
    val packedChunkPos: Int,// ChunkPos.asInt
    val sectionY: Byte,
    val sectionRelativeBlockPos: Short,
    val stateID: Int,
) : SPacket {
    constructor(buf: RByteBuf): this(buf.readInt(),buf.readByte(),buf.readShort(),buf.readInt())
    override suspend fun handle(ctx: ChannelHandlerContext) {
        TODO("Not yet implemented")
    }

}