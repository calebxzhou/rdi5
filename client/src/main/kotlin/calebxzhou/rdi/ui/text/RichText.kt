package calebxzhou.rdi.ui.text

import calebxzhou.rdi.common.WHITE
import calebxzhou.rdi.ui.FONT
import calebxzhou.rdi.ui.UiWidth
import calebxzhou.rdi.ui.general.Icons
import calebxzhou.rdi.ui.matrixOp
import calebxzhou.rdi.ui.renderItemStack
import calebxzhou.rdi.util.*
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.MultiLineLabel
import net.minecraft.client.gui.components.PlayerFaceRenderer
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Item

//文字，以及插在文字中的小图标
typealias RichTextRenderer = RichText.Context.() -> Unit

class RichText(val x: Int, val y: Int, val plainText: MutableComponent,val renderers: List<RichTextRenderer>) {
    data class Context(
        val guiGraphics: GuiGraphics,
        var width: Int = 0,
        var height: Int = 0,
        var nowX: Int,
        var nowY: Int
    ) {
        val pose
            get() = guiGraphics.pose()

    }

    class Builder(val startX: Int, val startY: Int) {
        val ren = arrayListOf<RichTextRenderer>()
        var plainText = EmptyComp

        fun fill() {
            ren.add(0) {
                guiGraphics.fill(startX - 4, startY - 4, startX + width, startY + height, 0xAA000000.toInt())
            }
        }

        fun text(str: String, color: Int = WHITE) {
            plainText += str.mcComp
            ren += {
                val txt = str.mcComp
                val width =
                    MultiLineLabel.create(FONT, txt, UiWidth).renderLeftAligned(guiGraphics, nowX, nowY, 10, color)
                //val width = guiGraphics.drawString(FONT, txt, nowX, nowY, color)
                nowX += FONT.width(txt)
            }
        }

        fun text(txt: Component) {
            plainText += txt
            ren += {
                val width = MultiLineLabel.create(FONT, txt, UiWidth)
                    .renderLeftAligned(guiGraphics, nowX, nowY, 10, txt.style.color?.value ?: WHITE)
                //val width = guiGraphics.drawString(FONT, txt, nowX, nowY, txt.style.color?.value ?: WHITE)
                nowX += FONT.width(txt)
            }
        }

        fun player(skin: ResourceLocation) {
            ren += {
                PlayerFaceRenderer.draw(guiGraphics, skin, nowX, nowY, 12)
                nowX += 16
            }
        }

        fun item(item: Item) {
            item(item to 1)
        }

        fun item(item: LiteItemStack) {
            ren += {
                //nowX += 5
                guiGraphics.matrixOp {
                    pose.translate(nowX.toFloat(), nowY.toFloat() + 0, 0f)
                    guiGraphics.matrixOp {
                        guiGraphics.renderItemStack(itemStack = item.full, width = 12, height = 12)
                    }
                    //渲染数量
                    if (item.second > 1) {

                        pose.scale(0.9f, 0.9f, 1f)
                        pose.translate(2f, 0f, 1f)
                        guiGraphics.drawString(FONT, "${item.second}", 0, 0, WHITE)
                    }

                }
                nowX += 12
            }
            //物品的中文名
            text(item.first.description)
        }

        fun icon(name: String) {
            ren += {
                guiGraphics.matrixOp {
                    pose.translate(0f, -1f, 0f)
                    guiGraphics.blit(Icons[name], nowX, nowY, 0f, 0f, 10, 10, 10, 10)
                }
                nowX += 12
            }
        }

        //鼠标右键
        fun rmb() {
            plainText += "鼠标右键".mcComp
            icon("rmb")
        }

        //鼠标左键
        fun lmb() {
            plainText += "鼠标左键".mcComp
            icon("lmb")
        }
        fun lmbHold() {
            text("长按")
            plainText += "鼠标左键".mcComp
            icon("lmb")
        }

        /*fun key(name: String) {
            ren += {
                guiGraphics.matrixOp {
                    pose.translate(0f, -1f, 0f)
                    guiGraphics.blit(KeyboardIcons[name], nowX,nowY, 0f, 0f, 10, 10, 10, 10)
                }
                nowX += 12
            }
        }*/

        fun ret() {
            ren += {
                if (nowX > width)
                    width = nowX
                if (nowY > height)
                    height = nowY
                nowX = startX
                nowY += 12
            }
        }

        fun build(): RichText {
            //最后一行要换行才能正确计算高度+显示背景
            ret()
            return RichText(startX, startY, plainText,ren)
        }
    }

    var widthHeight = 0 to 0
    fun render(guiGraphics: GuiGraphics) {
        val ctx = Context(guiGraphics, widthHeight.first, widthHeight.second, x, y)
        guiGraphics.matrixOp {
            renderers.forEach { it(ctx) }
        }
        widthHeight = ctx.width to ctx.height
    }
}

fun richText(x: Int = 0, y: Int = 0, builder: RichText.Builder.() -> Unit): RichText {
    return RichText.Builder(x, y).apply(builder).build()
}
