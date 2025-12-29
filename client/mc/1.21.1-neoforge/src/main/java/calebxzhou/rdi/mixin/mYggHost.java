package calebxzhou.rdi.mixin;

import calebxzhou.rdi.client.mc.RDI;
import com.mojang.authlib.Environment;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.Proxy;

/**
 * calebxzhou @ 2025-12-14 21:39
 */
@Mixin(YggdrasilEnvironment.class)
public class mYggHost {

    @Overwrite(remap = false)
    public Environment getEnvironment() {
        return RDI.ENV;
    }
}

@Mixin(Minecraft.class)
 class mYggHost2 {
    @Redirect(method = "<init>",at= @At(value = "NEW", target = "(Ljava/net/Proxy;)Lcom/mojang/authlib/yggdrasil/YggdrasilAuthenticationService;"))
    private YggdrasilAuthenticationService RDI$determineEnvironment(Proxy proxy) {
        return new YggdrasilAuthenticationService(proxy,RDI.ENV);
    }
}
@Mixin(Environment.class)
class mYggHost3{
    @Overwrite(remap = false)
    public String sessionHost() {
        return RDI.IHQ_URL;
    }
    @Overwrite(remap = false)
    public String servicesHost() {
        return RDI.IHQ_URL;
    }

}