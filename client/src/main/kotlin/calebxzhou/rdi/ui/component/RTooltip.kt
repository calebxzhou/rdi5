package calebxzhou.rdi.ui.component

import calebxzhou.rdi.common.LIGHT_RED
import calebxzhou.rdi.common.LIGHT_YELLOW
import calebxzhou.rdi.common.PINE_GREEN
import net.minecraft.client.gui.GuiGraphics

object RTooltip {
    @JvmStatic
    fun renderBackground(guiGraphics: GuiGraphics, x: Int, y: Int, width: Int, height: Int) {
        val z = 1
        val x1 = x - 4
        val y1 = y - 4
        val x2 = x1 + width + 8
        val y2 = y1 + height + 8
        guiGraphics.fill(x1, y1, x2, y2, z, LIGHT_YELLOW)
        guiGraphics.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, z, LIGHT_RED)
        guiGraphics.fill(x1 + 2, y1 + 2, x2 - 2, y2 - 2, z, PINE_GREEN)

    }
}