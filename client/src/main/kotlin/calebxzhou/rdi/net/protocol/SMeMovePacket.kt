package calebxzhou.rdi.net.protocol

import calebxzhou.rdi.net.RByteBuf
import calebxzhou.rdi.net.SPacket
import net.minecraft.network.FriendlyByteBuf

class SMeMovePacket()  {
    data class Pos(
        val x: Float,
        val y: Float,
        val z: Float,
    ): SPacket {
        override fun write(buf: FriendlyByteBuf) {
            buf.writeFloat(x)
            buf.writeFloat(y)
            buf.writeFloat(z)
        }
    }
    data class Rot(
        val yr:Float,
        val xr:Float,
    ): SPacket {
        override fun write(buf: FriendlyByteBuf) {
            buf.writeFloat(yr)
            buf.writeFloat(xr)
        }
    }

}