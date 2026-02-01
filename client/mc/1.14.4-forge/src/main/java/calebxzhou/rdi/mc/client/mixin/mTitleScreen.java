package calebxzhou.rdi.mc.client.mixin;

import calebxzhou.rdi.mc.client.RDIMain;
import net.minecraft.client.gui.screen.MainMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.RenderSkybox;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static calebxzhou.rdi.mc.client.RDIMain.JOIN_BUTTON;

/**
 * calebxzhou @ 2025-04-15 10:32
 */
@Mixin(MainMenuScreen.class)
public class mTitleScreen extends Screen {
    @Shadow
    @Mutable @Final
    private static final ResourceLocation PANORAMA_OVERLAY = RDIMain.BG_RES;
    protected mTitleScreen(ITextComponent title) {
        super(title);
    }

    @Redirect(method = "render",at= @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderSkybox;render(FF)V"))
    public void RDI$NOrenderPanorama(RenderSkybox instance, float p_217623_1_, float p_217623_2_) {

    }

    @Inject(method = "createNormalMenuOptions", at = @At("HEAD"))
    private void RDI$AddMultiplayerButton(int y, int rowHeight, CallbackInfo ci) {
         addButton(JOIN_BUTTON);
    }

}
