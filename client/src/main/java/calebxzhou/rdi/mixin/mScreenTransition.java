package calebxzhou.rdi.mixin;

import calebxzhou.rdi.RDI;
import calebxzhou.rdi.RDIKt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * calebxzhou @ 2025-04-16 11:29
 */
@Mixin(Minecraft.class)
public class mScreenTransition {
    @Shadow
    public Screen screen;

    @Inject(method = "setScreen",at=@At("HEAD"))
    private void RDI$ScreenTransition(Screen guiScreen, CallbackInfo ci){

        RDIKt.getLgr().info("画面迁移：{} -> {}", screen != null ? screen.getClass().getCanonicalName() : "null",guiScreen!=null ? guiScreen.getClass().getCanonicalName() : "null");
    }
}
