package calebxzhou.rdi.net

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import java.io.IOException

class RPacketDecoder : ByteToMessageDecoder() {
    override fun decode(
        ctx: ChannelHandlerContext,
        data: ByteBuf,
        out: MutableList<Any>
    ) {
        val len = data.readableBytes()
        if(len==0) return
        val buf = RByteBuf(data)
        val packetId = buf.readByte()
        val packet = RPacketSet.create(packetId,buf)
        if(packet==null){
            throw IOException("bad packet id $packetId")
        }
        val extraBytes = buf.readableBytes()
        if(extraBytes >0){
            throw IOException("packet${packetId} ${packet.javaClass.canonicalName} 有剩余${extraBytes}bytes没读完")
        }
        out += packet

    }
}
