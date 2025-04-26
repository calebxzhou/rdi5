package calebxzhou.rdi.ui.component.button

import calebxzhou.rdi.common.DARK_GRAY
import calebxzhou.rdi.common.WHITE
import calebxzhou.rdi.ui.Font
import calebxzhou.rdi.ui.UiWidth
import calebxzhou.rdi.ui.general.Icons
import calebxzhou.rdi.ui.general.Icons.draw
import calebxzhou.rdi.ui.matrixOp
import calebxzhou.rdi.ui.tran0ScaleBack
import calebxzhou.rdi.util.mcComp
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.MultiLineTextWidget


class RIconButton(
    val icon: String,
    var text: String? = null,
    val onClick: (Button) -> Unit
) : RButton(text, onClick) {
    val textWidget
        get() = text?.let { text -> MultiLineTextWidget(text.mcComp, Font).apply { setMaxWidth(UiWidth) } }
    //防止图标跟文字粘在一起
    val textOffsetX = Icons.SIZE+4//size /4
    val textOffsetY = 1//(size/4).toInt()

    companion object {
    }

    init {
        //图标肯定比文字大 放心把图标尺寸作为高度
        height = Icons.SIZE
        width = textOffsetX + ((textWidget?.width ?: 0) * scale).toInt()
        super.active = active
    }

    override fun renderWidget(gg: GuiGraphics, mouseX: Int, mouseY: Int, tick: Float) {
        //渲染文字
        textWidget?.let { tw ->

            gg.matrixOp {
                tran0ScaleBack(x + textOffsetX, y + textOffsetY, scale)
                tw.setColor(if (active) WHITE else DARK_GRAY)
                tw.render(gg, mouseX, mouseY, tick)
            }
        }
        gg.matrixOp {
            tran0ScaleBack(x  , y  , scale)
        Icons[icon].draw(gg, 0,0)
        }


    }
}