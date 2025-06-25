package calebxzhou.rdi.ihq.net.protocol

import calebxzhou.rdi.ihq.net.RByteBuf
import calebxzhou.rdi.ihq.net.SPacket
import io.netty.channel.ChannelHandlerContext


class SMeMovePacket()  {
    data class Pos(
        val x: Float,
        val y: Float,
        val z: Float,
    ): SPacket {
        constructor(buf: RByteBuf) : this(buf.readFloat(),buf.readFloat(),buf.readFloat())

        override suspend fun handle(ctx: ChannelHandlerContext) {
            TODO("Not yet implemented")
        }
    }
    data class Rot(
        val yr:Float,
        val xr:Float,
    ): SPacket {
        constructor(buf: RByteBuf) : this(buf.readFloat(),buf.readFloat())

        override suspend fun handle(ctx: ChannelHandlerContext) {
            TODO("Not yet implemented")
        }
    }

}