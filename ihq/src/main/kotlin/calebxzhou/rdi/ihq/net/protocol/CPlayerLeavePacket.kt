package calebxzhou.rdi.ihq.net.protocol

import calebxzhou.rdi.ihq.net.CPacket
import calebxzhou.rdi.ihq.net.RByteBuf

data class CPlayerLeavePacket(
    val tempUid: Byte
): CPacket {

    override fun write(buf: RByteBuf) {
        buf.writeByte(tempUid.toInt())
    }
}