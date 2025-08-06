package calebxzhou.rdi.mixin;

import calebxzhou.rdi.service.NetThrottler;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * calebxzhou @ 2024-06-27 19:28
 */

@Mixin(ServerCommonPacketListenerImpl.class)
class mNetThrottler {
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V",at=@At("HEAD"),cancellable = true)
    private void RDI$onSendPacket(Packet<?> packet, PacketSendListener listener, CallbackInfo ci){
        if(!NetThrottler.allowSendPacket((ServerCommonPacketListenerImpl)(Object)this,packet)){
            ci.cancel();
        }
    }
}