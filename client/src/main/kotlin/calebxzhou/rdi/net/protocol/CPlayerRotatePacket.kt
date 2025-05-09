package calebxzhou.rdi.net.protocol

import calebxzhou.rdi.net.CPacket
import io.netty.buffer.ByteBuf

class CPlayerRotatePacket(
    val tempUid: Byte,
    val yr: Float,
    val xr: Float,
) : CPacket {
    constructor(buf: ByteBuf): this(buf.readByte(),buf.readFloat(),buf.readFloat())

    override fun handle() {
        TODO()
    }

}
