package calebxzhou.rdi.mixin;

import calebxzhou.rdi.client.mc.RDI;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.SkinManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.CompletableFuture;

/**
 * calebxzhou @ 2025-08-04 16:45
 */
@Mixin(SkinManager.class)
public abstract class mSkinManager {

    @Redirect(method = "<init>",at= @At(value = "INVOKE", target = "Lcom/google/common/cache/CacheBuilder;build(Lcom/google/common/cache/CacheLoader;)Lcom/google/common/cache/LoadingCache;"))
    private LoadingCache<SkinManager.CacheKey, CompletableFuture<PlayerSkin>> RDI$Cache(CacheBuilder instance, CacheLoader loader){
        return instance.build(RDI.getSkinCache((SkinManager)(Object) this));
    }

}
