package calebxzhou.rdi.ihq.net

import calebxzhou.rdi.ihq.net.VarInt.writeVarInt
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler.Sharable
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.EncoderException
import io.netty.handler.codec.MessageToByteEncoder

@Sharable
class RLengthFieldPrepender : MessageToByteEncoder<ByteBuf>() {
    companion object {
        const val MAX_VARINT21_BYTES = 3
    }

    override fun encode(context: ChannelHandlerContext, msg: ByteBuf, out: ByteBuf) {
        val i = msg.readableBytes()
        val j = VarInt.getByteSize(i)
        if (j > 3) {
            throw EncoderException("Packet too large: size $i is over 8")
        } else {
            out.ensureWritable(j + i)
            out.writeVarInt(i)
            out.writeBytes(msg, msg.readerIndex(), i)
        }
    }
}