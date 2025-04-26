package calebxzhou.rdi.ui.component

import calebxzhou.rdi.ui.Font
import calebxzhou.rdi.ui.UiHeight
import calebxzhou.rdi.ui.UiWidth
import calebxzhou.rdi.util.EmptyComp
import calebxzhou.rdi.util.mcComp
import calebxzhou.rdi.util.rdiAsset

import kotlinx.serialization.json.JsonNull.content
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractScrollWidget
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.ImageWidget
import net.minecraft.client.gui.components.MultiLineTextWidget
import net.minecraft.client.gui.layouts.GridLayout
import net.minecraft.client.gui.layouts.LayoutSettings
import net.minecraft.client.gui.layouts.LinearLayout
import net.minecraft.client.gui.layouts.SpacerElement
import net.minecraft.client.gui.narration.NarratedElementType
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack

fun docWidget(
    x: Int = 0,
    y: Int = 20,
    width: Int = UiWidth - 5,
    height: Int = UiHeight - 25,
    builder: RDocWidget.Builder.() -> Unit
): RDocWidget {
    return RDocWidget.Builder(x, y, width, height).apply(builder).build()
}

class RDocWidget(pX: Int, pY: Int, pWidth: Int, pHeight: Int, val content: Content) :
    AbstractScrollWidget(pX, pY, pWidth, pHeight, "".mcComp) {
    override fun updateWidgetNarration(pNarrationElementOutput: NarrationElementOutput) {
        pNarrationElementOutput.add(NarratedElementType.TITLE, this.content.narration)
    }

    override fun getInnerHeight(): Int {
        return this.content.container.height
    }

    override fun scrollRate(): Double {
        return 9.0
    }


    override fun renderContents(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        val i = this.y + this.innerPadding()
        val j = this.x + this.innerPadding()
        pGuiGraphics.pose().pushPose()
        pGuiGraphics.pose().translate(j.toDouble(), i.toDouble(), 0.0)
        this.content.container.visitWidgets { widget: AbstractWidget ->
            widget.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
        }
        pGuiGraphics.pose().popPose()
    }

    data class Content(val container: GridLayout, val narration: Component)

    class Builder(val x: Int, val y: Int, val width: Int, val height: Int) {
        private val grid = GridLayout()
        private val helper: GridLayout.RowHelper
        private val alignHeader: LayoutSettings
        private val narration: MutableComponent = Component.empty()

        init {
            grid.defaultCellSetting().alignHorizontallyLeft()
            this.helper = grid.createRowHelper(1)
            helper.addChild(SpacerElement.width(width))
            this.alignHeader = helper.newCellSettings().alignHorizontallyCenter().paddingHorizontal(32)
        }


        fun spacer(pHeight: Int) {
            helper.addChild(SpacerElement.height(pHeight))
        }

        fun build(): RDocWidget {
            grid.arrangeElements()
            return RDocWidget(x, y, width, height, Content(this.grid, this.narration))
        }

        //一级标题 居中 绿粗
        fun h1(str: String) {
            helper.addChild(
                MultiLineTextWidget(
                    str.mcComp.withStyle(ChatFormatting.BOLD).withStyle(ChatFormatting.GREEN),
                    Font
                ).setMaxWidth(this.width - 64).setCentered(true),
                this.alignHeader.paddingTop(8).paddingBottom(8)
            )
            narration.append(content).append("\n")
        }

        //二级标题 居左 蓝
        fun h2(str: String) {
            helper.addChild(
                MultiLineTextWidget(
                    str.mcComp.withStyle(ChatFormatting.AQUA),
                    Font
                ).setMaxWidth(this.width - 64).setCentered(false),
                helper.newCellSettings().paddingTop(4).paddingBottom(4)
                // this.alignHeader
            )
            narration.append(content).append("\n")
        }

        //3级标题 居中 白
        fun h3(str: String) {
            helper.addChild(
                MultiLineTextWidget(
                    str.mcComp,
                    Font
                ).setMaxWidth(this.width - 64).setCentered(true),
                this.alignHeader.paddingTop(2).paddingBottom(2)
                // this.alignHeader
            )
            narration.append(content).append("\n")
        }

        //正文 首行2缩进关
        fun p(str: String, first2ch: Boolean = false) {
            helper.addChild(
                MultiLineTextWidget(
                    (if (first2ch) "　　".mcComp else EmptyComp.append(str.mcComp)),
                    Font
                ).setMaxWidth(this.width),
                helper.newCellSettings().paddingTop(2).paddingBottom(2)
            )
            narration.append(content).append("\n")
        }

        //图片
        fun img(path: String, width: Int = 128, height: Int = 128) {
            /*helper.addChild(
                ImageWidget.Texture(width, height, rdiAsset("textures/${path}.png")),
                this.alignHeader.paddingTop(3).paddingBottom(3)
            )*/
        }

        //物品
        fun items(vararg items: ItemStack) {
            val row = LinearLayout(items.size * 32, 0, LinearLayout.Orientation.HORIZONTAL)
            items.forEach {
                row.addChild(
                    RItemStackWidget(it, 32, 32)
                )
            }
            row.arrangeElements()
            helper.addChild(
                row,
                this.alignHeader.paddingTop(3).paddingBottom(3)
            )
        }
    }

}

