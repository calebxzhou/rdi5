package calebxzhou.rdi.ihq.net.protocol

import calebxzhou.rdi.ihq.net.RByteBuf
import calebxzhou.rdi.ihq.net.SPacket
import calebxzhou.rdi.ihq.net.protocol.SMeMovePacket.Pos


class SMeMovePacket()  {
    data class Pos(
        val x: Float,
        val y: Float,
        val z: Float,
    ): SPacket {
        constructor(buf: RByteBuf) : this(buf.readFloat(),buf.readFloat(),buf.readFloat())

        override fun handle() {
            TODO("Not yet implemented")
        }
    }
    data class Rot(
        val yr:Float,
        val xr:Float,
    ): SPacket {
        constructor(buf: RByteBuf) : this(buf.readFloat(),buf.readFloat())

        override fun handle() {
            TODO("Not yet implemented")
        }
    }

}