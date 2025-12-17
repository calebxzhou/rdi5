package calebxzhou.rdi.mixin;

import calebxzhou.rdi.client.mc.RDI;
import com.mojang.authlib.Environment;
import com.mojang.authlib.yggdrasil.YggdrasilEnvironment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * calebxzhou @ 2025-12-14 21:39
 */
@Mixin(YggdrasilEnvironment.class)
public class mYggHost {
    @Overwrite(remap = false)
    public Environment getEnvironment() {
        return new Environment(RDI.IHQ_URL,RDI.IHQ_URL,"PROD");
    }
}
