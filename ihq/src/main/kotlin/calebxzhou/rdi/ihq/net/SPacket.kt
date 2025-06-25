package calebxzhou.rdi.ihq.net

import io.netty.channel.ChannelHandlerContext

interface SPacket {
    suspend fun handle(ctx: ChannelHandlerContext)
}