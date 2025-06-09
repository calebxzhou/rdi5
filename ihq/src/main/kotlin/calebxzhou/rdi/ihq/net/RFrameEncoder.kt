package calebxzhou.rdi.ihq.net

import calebxzhou.rdi.ihq.lgr
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

class RFrameEncoder: MessageToByteEncoder<ByteBuf>() {
    override fun encode(
        ctx: ChannelHandlerContext,
        ln: ByteBuf,
        out: ByteBuf
    ) {
        val length = ln.readableBytes()
        val j = getVarIntByteSize(length)
        if (j > MAX_VARINT21_BYTES) {
            lgr.warn { "Packet too large: size $length is over 8" }
        } else {
            out.ensureWritable(j + length)
            out.writeVarInt(length)
            out.writeBytes(ln, ln.readerIndex(), length)
        }
    }
}