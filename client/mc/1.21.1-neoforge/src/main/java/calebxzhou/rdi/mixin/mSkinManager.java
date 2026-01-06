package calebxzhou.rdi.mixin;

import calebxzhou.rdi.mc.common.RDI;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.SkinManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * calebxzhou @ 2025-08-04 16:45
 */
@Mixin(SkinManager.class)
public abstract class mSkinManager {
    @Shadow
    abstract CompletableFuture<PlayerSkin> registerTextures(UUID uuid, MinecraftProfileTextures textures);

    @Unique
    private static Logger lgr = LogManager.getLogger("RDI.SkinManager");
    @Unique
    CacheLoader<SkinManager.CacheKey, CompletableFuture<PlayerSkin>> loader = new CacheLoader<>() {
        private final HttpClient httpClient = HttpClient.newHttpClient(); // Reusable client (thread-safe)
        private final Gson gson = new Gson();

        @Override
        public @NotNull CompletableFuture<PlayerSkin> load(SkinManager.CacheKey cacheKey) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RDI.getTextureQueryUrl(cacheKey.profileId(),"6")))
                    .GET()
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        String body = response.body();

                        if (response.statusCode() != 200) {
                            lgr.warn("Bad HTTP response {} for profile {} when fetching textures from IHQ",
                                    response.statusCode(), cacheKey.profileId());
                            return MinecraftProfileTextures.EMPTY;
                        }

                        try {
                            return gson.fromJson(body, MinecraftProfileTextures.class);
                        } catch (JsonParseException e) {
                            lgr.warn("Failed to parse textures JSON for profile {}: {}", cacheKey.profileId(), e.toString());
                            return MinecraftProfileTextures.EMPTY;
                        }
                    })
                    .exceptionally(throwable -> {
                        lgr.warn("Couldn't get textures from RDI IHQ server for profile {}: {}",
                                cacheKey.profileId(), throwable.toString());
                        return MinecraftProfileTextures.EMPTY;
                    })
                    .thenComposeAsync(textures ->
                                     registerTextures(cacheKey.profileId(), textures),
                            Minecraft.getInstance() // Assumes this provides the main/client thread executor
                    );
        }
    };
    @Redirect(method = "<init>",at= @At(value = "INVOKE", target = "Lcom/google/common/cache/CacheBuilder;build(Lcom/google/common/cache/CacheLoader;)Lcom/google/common/cache/LoadingCache;"))
    private LoadingCache<SkinManager.CacheKey, CompletableFuture<PlayerSkin>> RDI$Cache(CacheBuilder instance, CacheLoader loader){
        return instance.build(this.loader);
    }

}
