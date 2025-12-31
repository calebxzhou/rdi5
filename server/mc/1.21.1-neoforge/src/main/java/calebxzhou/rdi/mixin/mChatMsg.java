package calebxzhou.rdi.mixin;

import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * calebxzhou @ 2025-12-30 22:40
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class mChatMsg {
    @Shadow
    protected abstract void broadcastChatMessage(PlayerChatMessage message);

    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleChat",at=@At("HEAD"), cancellable = true)
    private void RDI$chat(ServerboundChatPacket packet, CallbackInfo ci){
        broadcastChatMessage(PlayerChatMessage.system(packet.message()));
        ci.cancel();
    }
}
