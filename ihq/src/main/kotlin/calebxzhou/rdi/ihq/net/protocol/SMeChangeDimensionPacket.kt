package calebxzhou.rdi.ihq.net.protocol

import calebxzhou.rdi.ihq.net.RByteBuf
import calebxzhou.rdi.ihq.net.SPacket
import calebxzhou.rdi.ihq.net.readString
import io.netty.channel.ChannelHandlerContext

data class SMeChangeDimensionPacket(
    val old: String,
    val now: String,
): SPacket{
    constructor(buf: RByteBuf) : this(buf.readString(),buf.readString())

    override suspend fun handle(ctx: ChannelHandlerContext) {
        TODO("Not yet implemented")
    }

}
