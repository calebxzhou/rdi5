package calebxzhou.rdi.mixin;

import calebxzhou.rdi.RDIKt;
import icyllis.modernui.mc.MuiScreen;
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
        String s1 = screen != null ? screen.getClass().getCanonicalName() : "null";
        if(screen instanceof MuiScreen){
            s1 += ":"+((MuiScreen) screen).getFragment().getClass().getCanonicalName();
        }
        String s2 = guiScreen != null ? guiScreen.getClass().getCanonicalName() : "null";
        if(guiScreen instanceof MuiScreen){
            s2 += ":"+((MuiScreen) guiScreen).getFragment().getClass().getCanonicalName();
        }
        RDIKt.getLgr().info("画面迁移：{} -> {}", s1, s2);
    }
}
