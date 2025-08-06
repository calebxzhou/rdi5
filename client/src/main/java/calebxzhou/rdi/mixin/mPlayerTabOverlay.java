package calebxzhou.rdi.mixin;

import calebxzhou.rdi.service.RPlayerTabOverlay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * calebxzhou @ 2024-05-23 12:23
 */

@Mixin(PlayerTabOverlay.class)
public class mPlayerTabOverlay {

    /**
     * @author calebxzhou
     * @reason 旁观模式不显示昵称
     */
   /* @Overwrite
    private Component decorateName(PlayerInfo entry, MutableComponent name) {
        return name;
    }

    //旁观模式不黑色昵称
    @Redirect(
            method = "render",
            at =@At(
                    value = "INVOKE",target = "Lnet/minecraft/client/multiplayer/PlayerInfo;getGameMode()Lnet/minecraft/world/level/GameType;"
            )
    )
    private GameType noSpecGray(PlayerInfo instance) {
        return GameType.SURVIVAL;
    }*/

    //离线登录也显示头像
    @Redirect(method = "render",
            at = @At(value = "INVOKE",target = "Lnet/minecraft/network/Connection;isEncrypted()Z"))
    private boolean alwaysDisplayAvatar(Connection instance){
        return true;
    }
    @Overwrite
    public void renderPingIcon(GuiGraphics pGuiGraphics, int p_281809_, int p_282801_, int pY, PlayerInfo pPlayerInfo) {
        RPlayerTabOverlay.renderPingIcon(pGuiGraphics,p_281809_,p_282801_,pY,pPlayerInfo);
    }

}