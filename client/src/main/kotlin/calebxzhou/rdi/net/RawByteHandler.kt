package calebxzhou.rdi.net

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelOutboundHandlerAdapter
import io.netty.channel.ChannelPromise


 class RawByteHandler : ChannelOutboundHandlerAdapter() {
    @Throws(Exception::class)
    override fun write(ctx: ChannelHandlerContext, msg: Any?, promise: ChannelPromise?) {
        if (msg is ByteBuf) {
            // If message is ByteBuf, write it directly
            ctx.write(msg, promise)
        } else {
            // Pass non-ByteBuf messages (e.g., custom objects) to next handler
            ctx.write(msg, promise)
        }
    }
}