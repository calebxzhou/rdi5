package calebxzhou.rdi.mixin;

import calebxzhou.rdi.model.Host;
import calebxzhou.rdi.net.RawByteHandler;
import calebxzhou.rdi.net.RawBytes;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import net.minecraft.network.BandwidthDebugMonitor;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * calebxzhou @ 2025-09-13 9:27
 */
@Mixin(Connection.class)
abstract
class mProxyConnect {
    @Shadow
    public abstract Channel channel();

    @Inject(method = "configureSerialization", at = @At(value = "INVOKE", target = "Lio/netty/channel/ChannelPipeline;addLast(Ljava/lang/String;Lio/netty/channel/ChannelHandler;)Lio/netty/channel/ChannelPipeline;", ordinal = 0))
    private static void RDI$RawByteEncoder(ChannelPipeline pipeline, PacketFlow flow, boolean memoryOnly,
            BandwidthDebugMonitor bandwithDebugMonitor, CallbackInfo ci) {
        pipeline.addFirst("rawByteHandler", new RawByteHandler());
    }
    //c->s第一个包是intention 发送之前决定proxy端口
    @Inject(method = "initiateServerboundConnection",
        at = @At(value = "HEAD")
            //        at = @At(  value = "INVOKE", target = "Lnet/minecraft/network/Connection;sendPacket(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;Z)V")
    )
    private void RDI$BeforeSendIntention(CallbackInfo ci) {
        try {
            var data = Unpooled.directBuffer();
            data.writeShort(Host.Companion.getNow().getPort());
            channel().writeAndFlush(new RawBytes(data));
            Thread.sleep(100);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
