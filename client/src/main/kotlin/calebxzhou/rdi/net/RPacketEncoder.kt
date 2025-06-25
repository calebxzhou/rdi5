package calebxzhou.rdi.net

import calebxzhou.rdi.lgr
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import io.netty.handler.codec.MessageToMessageEncoder

class RPacketEncoder : MessageToByteEncoder<SPacket>() {
    override fun encode(ctx: ChannelHandlerContext, packet: SPacket, data: ByteBuf) {
        RPacketSet.getPacketId(packet.javaClass)?.let { packetId ->

            val msg  = RByteBuf(data)
            //写包ID
            msg.writeByte(packetId.toInt())
            packet.write(msg)
            lgr.info("${packetId}")
        } ?: lgr.error("找不到包ID" + packet.javaClass)
    }
}