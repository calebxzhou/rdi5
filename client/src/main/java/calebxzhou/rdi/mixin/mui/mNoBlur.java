package calebxzhou.rdi.mixin.mui;

import icyllis.modernui.mc.BlurHandler;
import icyllis.modernui.mc.ScreenCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * calebxzhou @ 2025-08-07 19:28
 */
@Mixin(BlurHandler.class)
public class mNoBlur {
    //取消模糊效果
    @Redirect(method = "blur",at= @At(value = "INVOKE", target = "Licyllis/modernui/mc/ScreenCallback;shouldBlurBackground()Z"))
    private boolean RDI$NoBlurBackground(ScreenCallback instance) {
        return false;
    }
}
