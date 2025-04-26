package calebxzhou.rdi.net.protocol.async

import calebxzhou.rdi.net.CPacket
import io.netty.buffer.ByteBuf

class CPlayerMovePacket(
    val x: Float,
    val y: Float,
    val z: Float,
) : CPacket {
    constructor(buf: ByteBuf): this(buf.readFloat(),buf.readFloat(),buf.readFloat())

}
