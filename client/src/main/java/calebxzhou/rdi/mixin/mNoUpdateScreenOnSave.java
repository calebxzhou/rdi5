package calebxzhou.rdi.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

/**
 * calebxzhou @ 2025-07-30 22:59
 */
@Mixin(Minecraft
        .class)
public class mNoUpdateScreenOnSave {
  /*  @Redirect(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V",
    at= @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;updateScreenAndTick(Lnet/minecraft/client/gui/screens/Screen;)V"))
    private void RDI$NoUpdateScreenOnSave(Minecraft instance, Screen screen){

    }*/
}
