package calebxzhou.rdi.net.protocol.async

import calebxzhou.rdi.net.CPacket
import io.netty.buffer.ByteBuf

class CPlayerRotatePacket(
    val yr: Float,
    val xr: Float,
) : CPacket {
    constructor(buf: ByteBuf): this(buf.readFloat(),buf.readFloat())

}
