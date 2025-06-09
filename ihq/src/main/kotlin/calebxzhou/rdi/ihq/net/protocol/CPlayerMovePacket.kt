package calebxzhou.rdi.ihq.net.protocol

import calebxzhou.rdi.ihq.net.CPacket
import calebxzhou.rdi.ihq.net.RByteBuf

class CPlayerMovePacket {
    data class Pos(
        val tempUid: Byte,
        val x: Float,
        val y: Float,
        val z: Float,
    ): CPacket{


        override fun write(buf: RByteBuf) {
            buf.writeByte(tempUid.toInt()).writeFloat(x).writeFloat(y).writeFloat(z)
        }

    }
    data class Rot(
        val tempUid: Byte,
        val yr:Float,
        val xr:Float,
    ): CPacket{


        override fun write(buf: RByteBuf) {
            buf.writeByte(tempUid.toInt()).writeFloat(yr).writeFloat(xr)
        }

    }
}
