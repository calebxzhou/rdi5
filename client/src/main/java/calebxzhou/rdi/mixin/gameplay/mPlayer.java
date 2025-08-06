package calebxzhou.rdi.mixin.gameplay;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * calebxzhou @ 2024-06-05 19:43
 */
@Mixin(Player.class)
public class mPlayer {
    @Inject(method = "isModelPartShown", at = @At("HEAD"), cancellable = true)
//永远显示披风
    private void RDI$alwaysDisplayCape(PlayerModelPart pPart, CallbackInfoReturnable<Boolean> cir) {
        if (pPart == PlayerModelPart.CAPE) cir.setReturnValue(true);
    }
}
