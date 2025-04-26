package calebxzhou.rdi.mixin.event;

import calebxzhou.rdi.RGameEvents;
import calebxzhou.rdi.event.PacketSentEvent;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;

/**
 * calebxzhou @ 2025-04-20 18:52
 */
@Mixin(Connection.class)
public class mOnPacketSent {
    @Shadow @Nullable private volatile PacketListener packetListener;

    @Shadow private Channel channel;

    @Inject(method = "doSendPacket",at= @At( "TAIL"),locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private void RDI$OnPacketSend(Packet<?> p_packet, PacketSendListener sendListener, boolean flush, CallbackInfo ci, ChannelFuture channelfuture){
        //RGameEvents.e2(new PacketSentEvent(p_packet,sendListener,channel,channelfuture));
    }
}
