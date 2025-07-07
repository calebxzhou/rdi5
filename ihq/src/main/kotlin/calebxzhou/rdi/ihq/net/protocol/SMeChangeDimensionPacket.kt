package calebxzhou.rdi.ihq.net.protocol

import calebxzhou.rdi.ihq.REGEX_RESLOCA
import calebxzhou.rdi.ihq.lgr
import calebxzhou.rdi.ihq.net.RByteBuf
import calebxzhou.rdi.ihq.net.SPacket
import calebxzhou.rdi.ihq.net.account
import calebxzhou.rdi.ihq.net.readString
import io.netty.channel.ChannelHandlerContext

data class SMeChangeDimensionPacket(
    val old: String,
    val now: String,
): SPacket{
    constructor(buf: RByteBuf) : this(buf.readString(),buf.readString())

    override suspend fun handle(ctx: ChannelHandlerContext) {
        if(!REGEX_RESLOCA.matches(now)){
            lgr.warn { "维度id格式错误" }
            return
        }
        ctx.account?.gameContext?.dimension =now
    }

}
