package calebxzhou.rdi.ihq.net

import calebxzhou.rdi.ihq.lgr
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder

class RFrameDecoder : ByteToMessageDecoder() {
    companion object {
        private fun copyVarint(ln: ByteBuf, out: ByteBuf): Boolean {
            for (i in 0..2) {
                if (!ln.isReadable) {
                    return false
                }

                val b0 = ln.readByte()
                out.writeByte(b0.toInt())
                if (!hasVarIntContinuationBit(b0)) {
                    return true
                }
            }

            lgr.warn("length wider than 21-bit")
            return false
        }
    }

    private val helperBuf: ByteBuf = Unpooled.directBuffer(MAX_VARINT21_BYTES)
    override fun handlerRemoved0(context: ChannelHandlerContext?) {
        this.helperBuf.release()
    }

    override fun decode(
        ctx: ChannelHandlerContext,
        ln: ByteBuf,
        out: MutableList<Any>
    ) {
        ln.markReaderIndex()
        this.helperBuf.clear()
        if (!copyVarint(ln, this.helperBuf)) {
            ln.resetReaderIndex()
        } else {
            val i = helperBuf.readVarInt()
            if (ln.readableBytes() < i) {
                ln.resetReaderIndex()
            } else {
                out.add(ln.readBytes(i))
            }
        }
    }

}
