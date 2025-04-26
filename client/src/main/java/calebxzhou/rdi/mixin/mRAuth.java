package calebxzhou.rdi.mixin;

import calebxzhou.rdi.auth.RAuthService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.Proxy;

/**
 * calebxzhou @ 2025-04-26 21:38
 */
@Mixin(Minecraft.class)
public class mRAuth {
    @Redirect(method = "<init>", at = @At(value = "NEW", target = "com/mojang/authlib/yggdrasil/YggdrasilAuthenticationService"), remap = false)
    private YggdrasilAuthenticationService RDI$AuthService(Proxy proxy) {
        return new RAuthService();
    }
}
