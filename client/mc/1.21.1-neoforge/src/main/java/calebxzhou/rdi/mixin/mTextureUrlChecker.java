package calebxzhou.rdi.mixin;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import com.mojang.authlib.SignatureState;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.properties.Property;
import com.mojang.datafixers.kinds.K1;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.SkinManager;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * calebxzhou @ 2025-08-04 16:45
 */
@Mixin(SkinManager.class)
public abstract class mTextureUrlChecker {

    @Shadow
    abstract CompletableFuture<PlayerSkin> registerTextures(UUID uuid, MinecraftProfileTextures textures);

    @Redirect(method = "<init>",at= @At(value = "INVOKE", target = "Lcom/google/common/cache/CacheBuilder;build(Lcom/google/common/cache/CacheLoader;)Lcom/google/common/cache/LoadingCache;"))
    private LoadingCache<SkinManager.CacheKey, CompletableFuture<PlayerSkin>> RDI$Cache(CacheBuilder instance, CacheLoader loader){
        return instance.build(new CacheLoader<SkinManager.CacheKey, CompletableFuture<PlayerSkin>>() {
            @Override
            public CompletableFuture<PlayerSkin> load(SkinManager.CacheKey cacheKey) throws Exception {
                return CompletableFuture.supplyAsync(() -> getMinecraftProfileTexturesSupplier(cacheKey), Util.backgroundExecutor()).thenComposeAsync((p_307130_) -> registerTextures(cacheKey.profileId(), p_307130_), Minecraft.getInstance());

            }

        });
    }

    @Unique
    private @NotNull MinecraftProfileTextures getMinecraftProfileTexturesSupplier(SkinManager.CacheKey cacheKey) {

            Property property = cacheKey.packedTextures();
            if (property == null) {
                return MinecraftProfileTextures.EMPTY;
            } else {

                return new Gson().fromJson(new String(Base64.getDecoder().decode(property.value()), StandardCharsets.UTF_8), MinecraftProfileTextures.class);
            }

    }
}
