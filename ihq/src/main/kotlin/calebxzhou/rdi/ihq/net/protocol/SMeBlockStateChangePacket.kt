package calebxzhou.rdi.ihq.net.protocol

import calebxzhou.rdi.ihq.net.RByteBuf
import calebxzhou.rdi.ihq.net.SPacket
import calebxzhou.rdi.ihq.net.account
import calebxzhou.rdi.ihq.service.LevelService
import io.netty.channel.ChannelHandlerContext

class SMeBlockStateChangePacket(
    val packedChunkPos: Int,// ChunkPos.asInt
    val sectionY: Byte,
    val sectionRelativeBlockPos: Short,
    val stateID: Int,
) : SPacket {
    constructor(buf: RByteBuf) : this(buf.readInt(), buf.readByte(), buf.readShort(), buf.readInt())

    override suspend fun handle(ctx: ChannelHandlerContext) {
        LevelService.changeBlockState(this, ctx.account?.gameContext ?: return)
    }

}