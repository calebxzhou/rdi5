package calebxzhou.rdi.net.protocol

import calebxzhou.rdi.net.RByteBuf
import calebxzhou.rdi.net.SPacket

//成功连接以后服务端创建timer，5秒不发登录包-断开
/**
 * 登录游戏服务器
 */
class SMeJoinPacket(
    val qq: String,
    val pwd: String
): SPacket {
    override fun write(buf: RByteBuf) {
        buf.writeUtf(qq)
        buf.writeUtf(pwd)
    }
}