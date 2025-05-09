package calebxzhou.rdi.ui.general

import calebxzhou.rdi.ui.LineHeight
import calebxzhou.rdi.ui.matrixOp
import calebxzhou.rdi.ui.tran0ScaleBack
import calebxzhou.rdi.util.rdiAsset
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation

object Icons {
    operator fun get(name: String): ResourceLocation {
        return rdiAsset("textures/gui/icons/${name}.png")
    }
        val SIZE = LineHeight+2
    fun ResourceLocation.draw(guiGraphics: GuiGraphics,x:Int=0,y:Int=0,scale: Double = 1.0){
        guiGraphics.matrixOp {
            tran0ScaleBack(x,y,scale)
            guiGraphics.blit(this@draw, 0,0, 0f, 0f, SIZE,SIZE,SIZE,SIZE)
        }
    }
}