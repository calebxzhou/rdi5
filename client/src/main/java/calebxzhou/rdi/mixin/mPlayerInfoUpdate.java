package calebxzhou.rdi.mixin;

import calebxzhou.rdi.service.PlayerService;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * calebxzhou @ 2025-08-14 22:36
 */
@Mixin(ClientboundPlayerInfoUpdatePacket.class)
public class mPlayerInfoUpdate {
    //只要接收到玩家信息更新包 就立刻获取基本信息到缓存 不然载入皮肤会卡
    @Inject(method = "handle(Lnet/minecraft/network/protocol/game/ClientGamePacketListener;)V",at=@At("HEAD"))
    private void RDI$UpdatePlayerInfo(ClientGamePacketListener handler, CallbackInfo ci) {
        PlayerService.onPlayerInfoUpdate((ClientboundPlayerInfoUpdatePacket) (Object)this);
    }
}
