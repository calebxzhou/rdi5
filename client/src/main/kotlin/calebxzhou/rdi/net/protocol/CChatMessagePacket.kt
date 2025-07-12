package calebxzhou.rdi.net.protocol

import calebxzhou.rdi.net.CPacket
import calebxzhou.rdi.net.RByteBuf
import calebxzhou.rdi.net.readString

data class CChatMessagePacket(
    val msg: String
): CPacket {
    constructor(buf: RByteBuf) : this(buf.readString())
    override fun handle() {
        TODO("Not yet implemented")
    }

}
