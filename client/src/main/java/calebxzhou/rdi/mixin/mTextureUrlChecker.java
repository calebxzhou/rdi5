package calebxzhou.rdi.mixin;

import com.mojang.authlib.yggdrasil.TextureUrlChecker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * calebxzhou @ 2025-08-04 16:45
 */
@Mixin(TextureUrlChecker.class)
public class mTextureUrlChecker {
    @Overwrite
    public static boolean isAllowedTextureDomain(final String url) {return true;}
}
