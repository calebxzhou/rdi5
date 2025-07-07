package calebxzhou.rdi.ihq.net.protocol

import calebxzhou.rdi.ihq.net.CPacket
import calebxzhou.rdi.ihq.net.RByteBuf
//其他玩家离开服务器
data class CPlayerLeavePacket(
    val tmpId: Byte
): CPacket {

    override fun write(buf: RByteBuf) {
        buf.writeByte(tmpId.toInt())
    }
}