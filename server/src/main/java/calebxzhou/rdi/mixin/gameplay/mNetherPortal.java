package calebxzhou.rdi.mixin.gameplay;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * calebxzhou @ 2025-04-02 22:55
 */

@Mixin(value = {BaseFireBlock.class})
public class mNetherPortal {
    //到哪都能开地狱门
    @Inject(method = "inPortalDimension",at = @At("HEAD"), cancellable = true)
    private static void inPortalDimension(Level level, CallbackInfoReturnable<Boolean> cir) {
            cir.setReturnValue(true);
    }
}