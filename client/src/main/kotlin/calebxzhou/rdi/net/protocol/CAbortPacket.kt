package calebxzhou.rdi.net.protocol

import calebxzhou.rdi.net.CPacket
import calebxzhou.rdi.net.RByteBuf

data class CAbortPacket(
    val reason: String,
): CPacket {
    constructor(buf: RByteBuf):this(
        reason = buf.readUtf()
    )

    override fun handle() {
        TODO("Not yet implemented")
    }
}