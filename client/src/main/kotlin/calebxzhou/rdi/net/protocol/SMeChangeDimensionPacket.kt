package calebxzhou.rdi.net.protocol

import calebxzhou.rdi.net.RByteBuf
import calebxzhou.rdi.net.SPacket

data class SMeChangeDimensionPacket(
    val old: String,
    val now: String,
): SPacket{
    override fun write(buf: RByteBuf) {
        buf.writeUtf(old).writeUtf(now)
    }

}
