package calebxzhou.rdi.mixin;

import net.minecraft.server.dedicated.DedicatedServer;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * calebxzhou @ 2025-03-28 18:27
 */
@Mixin(DedicatedServer.class)
public class mDedicatedServer {
    @Redirect(method = "initServer",at= @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/common/ModConfigSpec$BooleanValue;get()Ljava/lang/Object;"))
    private Object RDI$NoLanPinger(ModConfigSpec.BooleanValue instance){
        return false;
    }
}
