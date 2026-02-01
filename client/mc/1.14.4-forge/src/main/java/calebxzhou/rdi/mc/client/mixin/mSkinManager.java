package calebxzhou.rdi.mc.client.mixin;

import calebxzhou.rdi.mc.common.RDI;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.minecraft.HttpMinecraftSessionService;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map;

/**
 * calebxzhou @ 2026-01-05 22:00
 */
@Mixin(SkinManager.class)
public abstract class mSkinManager {
    @Shadow
    protected abstract ResourceLocation registerTexture(MinecraftProfileTexture profileTexture, MinecraftProfileTexture.Type textureType, @Nullable SkinManager.ISkinAvailableCallback skinAvailableCallback);

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger(SkinManager.class);
    @Inject(method = "registerSkins",at=@At("HEAD"), cancellable = true)
    private void RDI$FetchRegisterSkins(GameProfile profile, SkinManager.ISkinAvailableCallback skinAvailableCallback, boolean requireSecure, CallbackInfo ci){
        Util.backgroundExecutor().execute(()->{
            String queryUrl = RDI.IHQ_URL+"/mc-profile/"+profile.getId()+"/clothes?authlibVer=4";
            try {
                HttpAuthenticationService service = ((HttpMinecraftSessionService) Minecraft.getInstance().getMinecraftSessionService()).getAuthenticationService();
                String resp = service.performGetRequest(HttpAuthenticationService.constantURL(queryUrl));
                LOGGER.info(resp);
                Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> map = new Gson().fromJson(
                    resp,
                    new TypeToken<Map<MinecraftProfileTexture.Type, MinecraftProfileTexture>>(){}.getType()
                );
                Minecraft.getInstance().execute(() -> {
                        ImmutableList.of(MinecraftProfileTexture.Type.SKIN, MinecraftProfileTexture.Type.CAPE).forEach((type) -> {
                            LOGGER.info("Fetched {} {} for {} from RDI", type, map.get(type),profile.getName());
                            if (map.containsKey(type)) {
                                LOGGER.info("Registering {} for {} from RDI", type,profile.getName());
                                registerTexture(map.get(type), type, skinAvailableCallback);
                            }


                    });
                });

            } catch (IOException e) {
                LOGGER.error("Failed to fetch skins from RDI: {}", e.getMessage());
                e.printStackTrace();
            }
        });
        ci.cancel();
    }
}
