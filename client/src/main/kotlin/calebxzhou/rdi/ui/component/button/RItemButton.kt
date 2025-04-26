package calebxzhou.rdi.ui.component.button

import calebxzhou.rdi.ui.drawText
import calebxzhou.rdi.ui.matrixOp
import calebxzhou.rdi.ui.renderItemStack
import calebxzhou.rdi.ui.tran0ScaleBack
import calebxzhou.rdi.ui.width
import calebxzhou.rdi.util.chineseName
import calebxzhou.rdi.util.id
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.world.item.Item

class RItemButton(
    val item: Item,
    onClick: (Button) -> Unit
): RButton(item.chineseName.string, onClick) {
    var size = 16
    init {
        width = size+2+item.chineseName.width
        height = size
    }
    override fun renderWidget(gg: GuiGraphics, mouseX: Int, mouseY: Int, tick: Float) {
        val textStartX = x + size
        gg.matrixOp {
            tran0ScaleBack(textStartX, y + 1, 0.9)
            gg.drawText(comp=item.chineseName)

        }
        gg.matrixOp {
            tran0ScaleBack(textStartX, y + 8, 0.5)
            gg.drawText(text = item.id.toString())
        }
        gg.matrixOp {
            gg.pose().translate(x.toFloat()-2, y.toFloat(), 1f)
            gg.renderItemStack(itemStack = item.defaultInstance, width =  size, height =  size)
        }
    }
}