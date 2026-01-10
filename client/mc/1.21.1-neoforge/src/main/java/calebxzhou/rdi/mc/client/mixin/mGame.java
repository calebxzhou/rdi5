package calebxzhou.rdi.mc.client.mixin;

import calebxzhou.rdi.mc.client.RMcSessionService;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * calebxzhou @ 2026-01-01 13:13
 */
@Mixin(Minecraft.class)
public class mGame {
    @Overwrite
    public boolean allowsTelemetry() {
        return false;
    }

    @Overwrite
    public boolean allowsMultiplayer() {
        return true;
    }
    @Redirect(method = "<init>",
            at = @org.spongepowered.asm.mixin.injection.At(value = "INVOKE",
                    target = "Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;createMinecraftSessionService()Lcom/mojang/authlib/minecraft/MinecraftSessionService;"))
    private MinecraftSessionService RDI$SessionService(YggdrasilAuthenticationService instance){
        return new RMcSessionService();
    }
    @Overwrite
    private UserApiService createUserApiService(YggdrasilAuthenticationService authenticationService, GameConfig gameConfig) {
        return UserApiService.OFFLINE;
    }
}
