package calebxzhou.rdi.ui.component.button

import calebxzhou.rdi.ui.LineHeight
import calebxzhou.rdi.ui.component.RightClickable
import calebxzhou.rdi.ui.width
import calebxzhou.rdi.util.mcComp
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button


open class RButton(
    text: String? = null,
    onClick: (Button) -> Unit={},
) : Button(
    0,
    0,
    (text?.width?:0)+20,
    //文字行高度+上下padding
    LineHeight + 3 * 2,
    (text?:"").mcComp,
    onClick,
    { (text?:"").mcComp.plainCopy() }
), RightClickable {
    var rightClick = { btn: RButton -> }
    var scale: Double = 1.0
    open var textComp = text?.mcComp?:"".mcComp
    override fun renderWidget(gg: GuiGraphics, mouseX: Int, mouseY: Int, tick: Float) {
        super.renderWidget(gg, mouseX, mouseY, tick)
    }

    override fun onRightClick(mouseX: Double, mouseY: Double) {
        rightClick(this)
    }

}