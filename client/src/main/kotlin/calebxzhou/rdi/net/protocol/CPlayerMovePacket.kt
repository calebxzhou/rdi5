package calebxzhou.rdi.net.protocol

import calebxzhou.rdi.net.CPacket
import calebxzhou.rdi.net.RByteBuf
import io.netty.buffer.ByteBuf

class CPlayerMovePacket {
    data class Pos(
        val tempUid: Byte,
        val x: Float,
        val y: Float,
        val z: Float,
    ): CPacket{
        constructor(buf: RByteBuf): this(buf.readByte(),buf.readFloat(),buf.readFloat(),buf.readFloat())
        override fun handle() {
            TODO("Not yet implemented")
        }

    }
    data class Rot(
        val tempUid: Byte,
        val yr:Float,
        val xr:Float,
    ): CPacket{
        constructor(buf: RByteBuf): this(buf.readByte(),buf.readFloat(),buf.readFloat())
        override fun handle() {
            TODO("Not yet implemented")
        }

    }
}
