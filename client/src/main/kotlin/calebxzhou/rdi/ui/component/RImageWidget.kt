package calebxzhou.rdi.ui.component

import calebxzhou.rdi.util.mcComp
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

class RImageWidget(
    x: Int=0,
    y: Int=0,
    width: Int=0,
    height: Int=0,
    val imgRL: ResourceLocation,
    val tooltip: Component?=null,
    val onClick: (RImageWidget) -> Unit={}
) : AbstractWidget(x,y,width,height, "图片".mcComp){
    init {
        super.tooltip = tooltip?.let {  Tooltip.create(it)  }
    }
    override fun onClick(mouseX: Double, mouseY: Double) {
        onClick(this)
    }

    override fun renderWidget(
        guiGraphics: GuiGraphics,
        mx: Int,
        my: Int,
        deltaT: Float
    ) {
        guiGraphics.blit(imgRL, x, y, 0f, 0f, width,height,width,height)
    }

    override fun updateWidgetNarration(pNarrationElementOutput: NarrationElementOutput) {

    }

}