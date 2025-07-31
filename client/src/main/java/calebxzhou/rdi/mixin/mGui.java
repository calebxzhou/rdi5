package calebxzhou.rdi.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * calebxzhou @ 2025-07-31 23:32
 */
@Mixin(Gui.class)
public class mGui {
    @Redirect(method = "renderTabList",at= @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isLocalServer()Z"))
    private boolean RDI$AlwaysDisplayTabPlayerList(Minecraft instance){
        return false;
    }
}
