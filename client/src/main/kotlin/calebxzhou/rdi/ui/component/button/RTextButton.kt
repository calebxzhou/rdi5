package calebxzhou.rdi.ui.component.button

import calebxzhou.rdi.ui.*
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.MutableComponent

class RTextButton(
    val text: String,
    onClick: (Button) -> Unit = {},
) : RButton(
    text, onClick
) {
    constructor(comp: MutableComponent,onClick: (Button) -> Unit):this(comp.string,onClick){
        textComp = comp
    }
    init {
        width = (textComp.width * scale).toInt()
        height = LineHeight
    }

    override fun renderWidget(gg: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        gg.matrixOp {
            tran0ScaleBack(x, y, scale)
            gg.drawText(comp = textComp)
        }
    }

}