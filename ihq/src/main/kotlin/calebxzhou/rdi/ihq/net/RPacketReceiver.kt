package calebxzhou.rdi.ihq.net

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RPacketReceiver : SimpleChannelInboundHandler<Any>() {
    private val packetHandleScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    override fun channelRead0(ctx: ChannelHandlerContext, packet: Any) {

        if (packet is SPacket) {
            packetHandleScope.launch {
                packet.handle(ctx)
            }
        }
    }


    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }

}