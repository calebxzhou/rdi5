package calebxzhou.rdi.service

import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.ui2.Icons
import calebxzhou.rdi.ui2.Icons.draw
import calebxzhou.rdi.util.*
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.PlayerFaceRenderer

object RGuiHud {


    @JvmStatic
    fun renderSelectedItemEnglishName(
        guiGraphics: GuiGraphics,
        langKey: String,
        cnX: Int,
        cnY: Int,
        alpha: Int
    ) {
        val color = 0xaaaaaa + (alpha shl 24)
        val comp = EnglishStorage[langKey].mcComp.withStyle(ChatFormatting.ITALIC)
        guiGraphics.drawString(Font, comp, (UiWidth - Font.width(comp)) / 2, cnY + 9, color)
    }




    fun onRender(gg: GuiGraphics) {
        NetMetrics.render(gg);
        gg.matrixOp {
            tran0ScaleBack(UiWidth - 40, UiHeight - 7, 0.75)
            Icons["qq"].draw(gg, scale = 0.8)
            gg.drawText("1095925708", x = 8, y = 0)
        }
        gg.matrixOp {
            tran0ScaleBack(0, UiHeight - 10, 0.7)
            RAccount.now?.let {
                PlayerFaceRenderer.draw(gg, it.cloth.skinLocation, 0, 0, 8)
                gg.drawText(it.name, x = 9)
                tran0ScaleBack(0, 8, 0.7)
                gg.drawText(comp = "RDID ${it._id}".toString().mcComp.withStyle(ChatFormatting.GRAY) )
            }
        }
        gg.matrixOp {
            tran0ScaleBack(0,-1,0.8)
            mc.player?.let { player ->
              //  val fps = "${mc.fps}f.".mcComp
                val x = "${if (player.x > 0) "+" else ""}${player.x.toFixed(1)}".mcComp.withStyle(ChatFormatting.GOLD)
                val y = "${if (player.y > 0) "+" else ""}${player.y.toFixed(1)}".mcComp.withStyle(ChatFormatting.GREEN)
                val z = "${if (player.z > 0) "+" else ""}${player.z.toFixed(1)}".mcComp.withStyle(ChatFormatting.AQUA)
                gg.drawText(comp=x+y+z)
            }
        }
    }





}