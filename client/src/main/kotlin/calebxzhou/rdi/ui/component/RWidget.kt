package calebxzhou.rdi.ui.component

import calebxzhou.rdi.util.mcComp
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component

abstract class RWidget(
    x: Int=0,
    y: Int=0,
    width: Int=0,
    height: Int=0,
    val tooltip: Component?=null
): AbstractWidget(x,y,width,height, "组件".mcComp) {
    init {
        super.tooltip = tooltip?.let {  Tooltip.create(it)  }
    }
    abstract fun doRenderWidget(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float
    )

    override fun renderWidget(guiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {

        doRenderWidget(guiGraphics,pMouseX,pMouseY,pPartialTick)
        /*if(mouseClicked(pMouseX.toDouble(), pMouseY.toDouble(), InputConstants.MOUSE_BUTTON_LEFT)){
            onClick(this)
        }*/
    }

    override fun updateWidgetNarration(pNarrationElementOutput: NarrationElementOutput) {

    }

}