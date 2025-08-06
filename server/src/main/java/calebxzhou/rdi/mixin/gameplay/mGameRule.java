package calebxzhou.rdi.mixin.gameplay;

import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * calebxzhou @ 2025-03-25 22:11
 */
@Mixin(GameRules.class)
public class mGameRule {
    @Inject(method = "getBoolean",at=@At("HEAD"), cancellable = true)
    private void alwaysKeepInv(GameRules.Key<GameRules.BooleanValue> pKey, CallbackInfoReturnable<Boolean> cir){
        if(pKey == GameRules.RULE_KEEPINVENTORY)
            cir.setReturnValue(true);
    }
}
