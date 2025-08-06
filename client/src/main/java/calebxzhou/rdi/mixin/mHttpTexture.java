package calebxzhou.rdi.mixin;

import calebxzhou.rdi.service.RSkinService;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.HttpTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * calebxzhou @ 2025-03-16 11:54
 */
@Mixin(HttpTexture.class)
public class mHttpTexture {


    @Redirect(method = "load(Ljava/io/InputStream;)Lcom/mojang/blaze3d/platform/NativeImage;",at= @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/HttpTexture;processLegacySkin(Lcom/mojang/blaze3d/platform/NativeImage;)Lcom/mojang/blaze3d/platform/NativeImage;"))
    private NativeImage RDI$LoadLegacySkin(HttpTexture instance, NativeImage flag){

        return RSkinService.processLegacySkin(flag);
    }
}
