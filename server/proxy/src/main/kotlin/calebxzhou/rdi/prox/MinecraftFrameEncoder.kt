package calebxzhou.rdi.prox

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

// Minecraft VarInt frame encoder - prepends packet length as VarInt
class MinecraftFrameEncoder : MessageToByteEncoder<ByteBuf>() {
    override fun encode(ctx: ChannelHandlerContext, msg: ByteBuf, out: ByteBuf) {
        val length = msg.readableBytes()
        writeVarInt(length, out)
        out.writeBytes(msg)
    }

    private fun writeVarInt(value: Int, buffer: ByteBuf) {
        var v = value
        while (true) {
            if ((v and 0x7F.inv()) == 0) {
                buffer.writeByte(v)
                return
            }
            buffer.writeByte((v and 0x7F) or 0x80)
            v = v ushr 7
        }
    }
}