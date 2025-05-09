package calebxzhou.rdi.net.protocol

import calebxzhou.rdi.net.CPacket
import io.netty.buffer.ByteBuf

class CPlayerMovePacket(
    val tempUid: Byte,
    val x: Float,
    val y: Float,
    val z: Float,
) : CPacket {
    constructor(buf: ByteBuf) : this(buf.readByte(), buf.readFloat(), buf.readFloat(), buf.readFloat())

    override fun handle() {
        TODO()
    }

}
