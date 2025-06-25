package calebxzhou.rdi.ihq.net.protocol

import calebxzhou.rdi.ihq.net.RByteBuf
import calebxzhou.rdi.ihq.net.SPacket
import calebxzhou.rdi.ihq.net.readString
import calebxzhou.rdi.ihq.service.PlayerService
import calebxzhou.rdi.ihq.service.PlayerService.goOnline
import io.netty.channel.ChannelHandlerContext

//成功连接以后服务端创建timer，5秒不发登录包-断开
/**
 * 登录游戏服务器
 */
class SMeLoginPacket(
    val qq: String,
    val pwd: String
): SPacket {
    constructor(buf: RByteBuf): this(buf.readString(),buf.readString())

    override suspend fun handle(ctx: ChannelHandlerContext) {
        PlayerService.getByQQ(qq)?.let {
            if (it.pwd == pwd) {
                it.goOnline(ctx)
            }
        }
    }
}