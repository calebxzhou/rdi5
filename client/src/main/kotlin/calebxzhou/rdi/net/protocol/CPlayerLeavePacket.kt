package calebxzhou.rdi.net.protocol

import calebxzhou.rdi.net.CPacket
import calebxzhou.rdi.net.RByteBuf

data class CPlayerLeavePacket(
    val tempUid: Byte
): CPacket {
    constructor(buf: RByteBuf): this(
        tempUid = buf.readByte()
    )
    override fun handle() {
        TODO("Not yet implemented")
    }
}