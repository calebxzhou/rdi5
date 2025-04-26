package calebxzhou.rdi.mixin;

import calebxzhou.rdi.service.LevelService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * calebxzhou @ 2025-04-15 10:32
 */
@Mixin(TitleScreen.class)
public class mTitleScreen {
    /*@Inject(method = "init",at=@At("HEAD"), cancellable = true)
    private void RDI$TitleScreen(CallbackInfo ci){
        MuiForgeApi.openScreen(new RTitleFrag());
        ci.cancel();
    }*/
    @Redirect(method = "lambda$createNormalMenuOptions$8",at= @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"))
    private void startRDI(Minecraft instance, Screen old){
        LevelService.start();
    }
}
