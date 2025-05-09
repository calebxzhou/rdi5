package calebxzhou.rdi.ui.component

import calebxzhou.rdi.common.WHITE
import calebxzhou.rdi.ui.FONT
import calebxzhou.rdi.ui.matrixOp
import calebxzhou.rdi.util.mcAsset
import calebxzhou.rdi.util.mcComp
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Checkbox

class RCheckbox(
    message: String,
    selected: Boolean = false,
    x: Int = 0,
    y: Int = 0,
    onChange: (Checkbox, Boolean) -> Unit = {a,b->}
) :
    Checkbox(
        x,
        y,
        FONT.width(message) + 50,
        message.mcComp,
        FONT,
        selected,
        onChange
    ) {
    public override fun renderWidget(gg: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        val minecraft = Minecraft.getInstance()
        RenderSystem.enableDepthTest()
        val font = minecraft.font
        gg.setColor(1.0f, 1.0f, 1.0f, this.alpha)
        RenderSystem.enableBlend()
        gg.matrixOp {
            gg.setColor(1.0f, 1.0f, 1.0f, 1.0f)
            gg.drawString(
                font,
                message,
                x+16,
                y+2,
                WHITE
            )
            translate(0f, 0f, 1f)
            scale(0.6f, 0.6f, 1f)
            translate(x / 0.6f, y / 0.6f, 1f)
            gg.blit(
                mcAsset("textures/gui/checkbox.png"),
                0,
                0,
                if (isFocused) 20.0f else 0.0f,
                if (selected()) 20.0f else 0.0f,
                20,
                height,
                64,
                64
            )

        }

    }

}