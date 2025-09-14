package calebxzhou.rdi.net

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder
import io.netty.util.ReferenceCountUtil

// Wrapper type to explicitly mark raw outbound frames we want to inject.
class RawBytes(val buf: ByteBuf)

// Encoder that only triggers for RawBytes; normal Packets bypass this handler entirely.
class RawByteHandler : MessageToByteEncoder<RawBytes>() {
    override fun encode(ctx: ChannelHandlerContext, msg: RawBytes, out: ByteBuf) {
        out.writeBytes(msg.buf)
        // Release the wrapped buffer to avoid leaks
        ReferenceCountUtil.safeRelease(msg.buf)
    }
}