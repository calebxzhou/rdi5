package calebxzhou.rdi.ui.general

import calebxzhou.rdi.util.mc.LineHeight
import calebxzhou.rdi.util.mc.matrixOp
import calebxzhou.rdi.util.mc.tran0ScaleBack
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation

object Icons {
    operator fun get(name: String): ResourceLocation {
        return ResourceLocation("rdi","textures/gui/icons/${name}.png")
    }
        val SIZE = LineHeight+2
    fun ResourceLocation.draw(guiGraphics: GuiGraphics,x:Int=0,y:Int=0,scale: Double = 1.0){
        guiGraphics.matrixOp {
            tran0ScaleBack(x,y,scale)/*
            translate(0f,0f,1f)
            scale(0.20f,0.20f,1f)
            translate(x.toFloat()*4,y.toFloat()*4,1f)*/
            guiGraphics.blit(this@draw, 0,0, 0f, 0f, SIZE,SIZE,SIZE,SIZE)
        }
    }
}