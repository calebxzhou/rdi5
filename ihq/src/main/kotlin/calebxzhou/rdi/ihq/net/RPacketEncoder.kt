package calebxzhou.rdi.ihq.net

import calebxzhou.rdi.ihq.lgr
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

class RPacketEncoder : MessageToByteEncoder<CPacket>() {
    override fun encode(ctx: ChannelHandlerContext, packet: CPacket, data: ByteBuf) {
        RPacketSet.getPacketId(packet.javaClass)?.let { packetId ->
            //写包ID
            data.writeByte(packetId.toInt())
            packet.write(data)

        } ?: lgr.error("找不到包ID" + packet.javaClass)
    }
}