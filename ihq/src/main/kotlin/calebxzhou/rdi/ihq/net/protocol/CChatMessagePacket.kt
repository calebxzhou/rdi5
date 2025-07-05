package calebxzhou.rdi.ihq.net.protocol

import calebxzhou.rdi.ihq.net.CPacket
import calebxzhou.rdi.ihq.net.RByteBuf
import calebxzhou.rdi.ihq.net.writeString

data class CChatMessagePacket(
    val msg: String
): CPacket{
    override fun write(buf: RByteBuf) {
        buf.writeString(msg)
    }

}
