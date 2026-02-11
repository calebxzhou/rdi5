package calebxzhou.rdi.mc.client.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.HttpTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * calebxzhou @ 2025-03-16 11:54
 */
@Mixin(HttpTexture.class)
public class mHttpTexture {

    /*@Inject(method = "processLegacySkin",at= @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/NativeImage;close()V",shift = At.Shift.BEFORE,ordinal = 1), cancellable = true)
    private void RDI$AllowHDSkin(NativeImage image, CallbackInfoReturnable<NativeImage> cir){
        //如果超过64x32尺寸的皮肤,不close 不return null 而是返回处理后的高清皮肤
        cir.setReturnValue(image);
    }*/
}
