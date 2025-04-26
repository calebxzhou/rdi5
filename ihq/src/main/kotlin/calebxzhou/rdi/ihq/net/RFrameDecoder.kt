package calebxzhou.rdi.ihq.net

import calebxzhou.rdi.ihq.net.VarInt.readVarInt
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.CorruptedFrameException

class RFrameDecoder( ) : ByteToMessageDecoder() {
    private val helperBuf: ByteBuf = Unpooled.directBuffer(3)

    override fun handlerRemoved0(context: ChannelHandlerContext) {
        helperBuf.release()
    }

    private fun copyVarint(input: ByteBuf, output: ByteBuf): Boolean {
        for (i in 0 until 3) {
            if (!input.isReadable) {
                return false
            }
            val b0 = input.readByte()
            output.writeByte(b0.toInt())
            if (!VarInt.hasContinuationBit(b0)) {
                return true
            }
        }
        throw CorruptedFrameException("length wider than 21-bit")
    }

    override fun decode(context: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>) {
        input.markReaderIndex()
        helperBuf.clear()
        if (!copyVarint(input, helperBuf)) {
            input.resetReaderIndex()
        } else {
            val i = helperBuf.readVarInt()
            if (input.readableBytes() < i) {
                input.resetReaderIndex()
            } else {
                out.add(input.readBytes(i))
            }
        }
    }
}