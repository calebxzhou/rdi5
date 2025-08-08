package calebxzhou.rdi.mixin.event;

import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

/**
 * calebxzhou @ 2025-04-20 18:52
 */
@Mixin(Connection.class)
public class mOnPacketSent {
    @Shadow @Nullable private volatile PacketListener packetListener;

    @Shadow private Channel channel;

 /*   @Inject(method = "doSendPacket",at= @At( "TAIL"),locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void RDI$OnPacketSend(Packet<?> p_packet, PacketSendListener sendListener, boolean flush, CallbackInfo ci, ChannelFuture channelfuture){
        RGameEvents.onPacketSent(new PacketSentEvent(p_packet,sendListener,channel,channelfuture));
    }*/
}
