package calebxzhou.rdi.mixin;

import calebxzhou.rdi.ui2.frag.PauseFragment;
import icyllis.modernui.mc.neoforge.MuiForgeApi;
import net.minecraft.client.gui.screens.PauseScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * calebxzhou @ 2025-07-30 21:57
 */
@Mixin(PauseScreen.class)
public class mPauseScreen {
    @Inject(method = "init",at=@At("TAIL"))
    private void RDI$goPauseScreen(CallbackInfo ci){
        MuiForgeApi.openScreen(new PauseFragment());
    }
}
