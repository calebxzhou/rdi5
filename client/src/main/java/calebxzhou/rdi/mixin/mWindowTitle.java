package calebxzhou.rdi.mixin;

import calebxzhou.rdi.Const;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * calebxzhou @ 2025-04-14 23:48
 */

@Mixin(Minecraft.class)
public class mWindowTitle {
    @Inject(method = "createTitle",at=@At("HEAD"), cancellable = true)
    private void RDI$CreateTitle(CallbackInfoReturnable<String> cir){
        cir.setReturnValue(Const.getVERSION_DISP());
    }
}
