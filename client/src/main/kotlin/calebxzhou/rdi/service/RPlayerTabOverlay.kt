package calebxzhou.rdi.service

import calebxzhou.rdi.util.drawText
import calebxzhou.rdi.util.matrixOp
import calebxzhou.rdi.util.mcComp
import calebxzhou.rdi.util.translate
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.multiplayer.PlayerInfo

object RPlayerTabOverlay {
    @JvmStatic
    fun renderPingIcon(
        gg: GuiGraphics,
        x1: Int,
        x2: Int,
        pY: Int,
        pi: PlayerInfo
    ) {
        val (vOffset,color) = if (pi.latency < 0) {
            5 to ChatFormatting.WHITE
        } else if (pi.latency < 150) {
            0 to ChatFormatting.GREEN
        } else if (pi.latency < 300) {
            1 to ChatFormatting.GOLD
        } else if (pi.latency < 600) {
            2 to ChatFormatting.RED
        } else if (pi.latency < 1000) {
            3 to ChatFormatting.DARK_RED
        } else {
            4 to ChatFormatting.BLACK
        }
        gg.matrixOp {
            translate(x2 + x1 - 11, pY,100)
           // gg.blit(RGuiHud.MC_ICONS,0,0 , 0, 176 + vOffset * 8, 10, 8)
            gg.drawText(comp=".${pi.latency}".mcComp.withStyle(color))
        }
    }
}