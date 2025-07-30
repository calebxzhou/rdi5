package calebxzhou.rdi.mixin;

import calebxzhou.rdi.ui2.frag.TitleFragment;
import icyllis.modernui.mc.neoforge.MuiForgeApi;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * calebxzhou @ 2025-04-15 10:32
 */
@Mixin(TitleScreen.class)
public class mTitleScreen {
    @Inject(method = "init",at=@At("HEAD"), cancellable = true)
    private void RDI$TitleScreen(CallbackInfo ci){
        //Minecraft.getInstance().setScreen(new RTitleScreen());
        MuiForgeApi.openScreen(new TitleFragment());
        ci.cancel();
    }

}
