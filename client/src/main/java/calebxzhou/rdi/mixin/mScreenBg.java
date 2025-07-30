package calebxzhou.rdi.mixin;

import calebxzhou.rdi.ui2.Ui2UtilsKt;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/**
 * calebxzhou @ 2025-07-25 23:12
 */
@Mixin(Screen.class)
public class mScreenBg {
    @Shadow public int width;

    @Shadow public int height;

    @Overwrite
    public void renderPanorama(GuiGraphics guiGraphics, float partialTick) {
        guiGraphics.blit(Ui2UtilsKt.getBG_IMAGE_MC(), 0, 0, 0f, 0f, width,height,width,height);

    }
}
