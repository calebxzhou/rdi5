package calebxzhou.rdi.ihq.net.protocol

import calebxzhou.rdi.ihq.net.RByteBuf
import calebxzhou.rdi.ihq.net.SPacket
import calebxzhou.rdi.ihq.net.readString

data class SMeChangeDimensionPacket(
    val old: String,
    val now: String,
): SPacket{
    constructor(buf: RByteBuf) : this(buf.readString(),buf.readString())

    override fun handle() {
        TODO("Not yet implemented")
    }

}
