package calebxzhou.rdi.ihq.net.protocol

import calebxzhou.rdi.ihq.net.RByteBuf
import calebxzhou.rdi.ihq.net.SPacket
import calebxzhou.rdi.ihq.net.account
import calebxzhou.rdi.ihq.service.PlayerService
import calebxzhou.rdi.ihq.service.PlayerService.goOffline
import io.netty.channel.ChannelHandlerContext

/**
 * 离开服务器
 */
class SMeLeavePacket(buf: RByteBuf): SPacket {
    override suspend  fun handle(ctx: ChannelHandlerContext) {
        ctx.account?.goOffline()

    }

}