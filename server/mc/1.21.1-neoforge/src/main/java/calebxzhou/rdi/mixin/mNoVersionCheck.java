package calebxzhou.rdi.mixin;

import net.neoforged.neoforge.common.NeoForgeMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * calebxzhou @ 2025-03-17 11:56
 */
@Mixin(net.neoforged.fml.VersionChecker.class)
public class mNoVersionCheck {
    @Overwrite(remap = false)
    public static void startVersionCheck() {}
}
@Mixin(NeoForgeMod.class)
class mNoVersionCheck2{

    @Redirect(method = "preInit",remap = false, at= @At(value = "INVOKE", target = "Lnet/neoforged/fml/VersionChecker;startVersionCheck()V"))
    private void RDI$NoStartCheck(){

    }
}