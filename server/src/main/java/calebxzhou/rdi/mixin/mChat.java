package calebxzhou.rdi.mixin;

import calebxzhou.rdi.service.PlayerService;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * calebxzhou @ 2024-06-14 14:47
 */

@Mixin(ServerGamePacketListenerImpl.class)
public class mChat {
    @Shadow public ServerPlayer player;

    @Inject(method = "handleChat",at=@At("HEAD"), cancellable = true)
    private void chatting(ServerboundChatPacket packet, CallbackInfo ci){

        PlayerService.INSTANCE.onChat(player, packet.message());
        ci.cancel();
    }
}
