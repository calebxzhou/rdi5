package calebxzhou.rdi.mixin;

import calebxzhou.rdi.ui.RMouseCursor;
import calebxzhou.rdi.ui.UiUtilsKt;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * calebxzhou @ 2024-06-17 7:59
 */
//鼠标进入组件时 改变光标形状

@Mixin(Screen.class)
public abstract class mWidgetMouseCursor {


    @Shadow
    @Final
    private List<GuiEventListener> children;

    @Inject(method = "render", at = @At(value = "TAIL", target = "Lnet/minecraft/client/gui/components/Renderable;render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
    private void RDI_onWidgetRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        var mcHwnd = UiUtilsKt.getWindowHandle();
        for (GuiEventListener widget : children) {
            if (widget.isMouseOver(mouseX, mouseY)) {
                if (widget instanceof Button) {
                    GLFW.glfwSetCursor(mcHwnd, RMouseCursor.handCursor);
                } else if (widget instanceof EditBox) {
                    GLFW.glfwSetCursor(mcHwnd, RMouseCursor.ibeamCursor);
                } else {
                    GLFW.glfwSetCursor(mcHwnd, RMouseCursor.handCursor);
                }
                break;
            } else {
                GLFW.glfwSetCursor(mcHwnd, RMouseCursor.arrowCursor);
            }
        }
    }
}
