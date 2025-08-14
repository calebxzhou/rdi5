package calebxzhou.rdi.mixin;

import calebxzhou.rdi.service.PlayerService;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.Property;
import net.minecraft.client.resources.SkinManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * calebxzhou @ 2025-08-13 14:26
 */
@Mixin(SkinManager.class)
public class mSkin {
    @Redirect(method = "getOrLoad",at= @At(value = "INVOKE", target = "Lcom/mojang/authlib/minecraft/MinecraftSessionService;getPackedTextures(Lcom/mojang/authlib/GameProfile;)Lcom/mojang/authlib/properties/Property;"))
    private Property RDI$PackedTextures(MinecraftSessionService instance, GameProfile gameProfile) {
        return PlayerService.getPackedTextures(gameProfile);
    }
}
