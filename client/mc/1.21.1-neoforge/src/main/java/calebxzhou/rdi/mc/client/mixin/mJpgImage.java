package calebxzhou.rdi.mc.client.mixin;

import calebxzhou.rdi.mc.common.JpegUtils;
import calebxzhou.rdi.mc.common.RDI;
import com.mojang.blaze3d.platform.NativeImage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * calebxzhou @ 8/17/2025 3:38 PM
 */
@Mixin(NativeImage.class)
public class mJpgImage {
    @Redirect(method = "read(Lcom/mojang/blaze3d/platform/NativeImage$Format;Ljava/nio/ByteBuffer;)Lcom/mojang/blaze3d/platform/NativeImage;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/PngInfo;validateHeader(Ljava/nio/ByteBuffer;)V"))
    private static void RDI$AlsoValidateJpegHeader(ByteBuffer buffer) {
        //跳过png验证 允许读jpg
    }
}
