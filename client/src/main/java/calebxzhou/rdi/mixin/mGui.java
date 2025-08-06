package calebxzhou.rdi.mixin;

import calebxzhou.rdi.service.RGuiHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * calebxzhou @ 2025-07-31 23:32
 */
@Mixin(Gui.class)
public abstract class mGui {
    @Redirect(method = "renderTabList",at= @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;isLocalServer()Z"))
    private boolean RDI$AlwaysDisplayTabPlayerList(Minecraft instance){
        return false;
    }  @Shadow
    public abstract Font getFont();

    @Shadow protected ItemStack lastToolHighlight;

    @Inject(method = "renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;I)V",locals = LocalCapture.CAPTURE_FAILHARD,
            at= @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawStringWithBackdrop(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)I"))
    private void RDI_renderSelectedItemEnglishName(GuiGraphics guiGraphics, int yShift, CallbackInfo ci, MutableComponent mutablecomponent, Component highlightTip, int i, int j, int k, int l, Font font){
        String id = lastToolHighlight.getDescriptionId();
        RGuiHud.renderSelectedItemEnglishName(guiGraphics,id,j,k,l);
    }
}
