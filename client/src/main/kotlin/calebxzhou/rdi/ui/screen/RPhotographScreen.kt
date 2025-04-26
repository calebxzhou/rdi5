package calebxzhou.rdi.ui.screen

import calebxzhou.rdi.ui.component.RScreen
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.mc.drawText
import calebxzhou.rdi.util.pressingKey
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.Camera
import net.minecraft.client.gui.GuiGraphics

class RPhotographScreen : RScreen("摄影") {
    companion object{
        const val DEFAULT_MOVE_FACTOR = 0.2
        var cameraOp = { cam : Camera ->

        }
    }
    override var showTitle = false
    override var clearColor=false
    var hOff: Double = 0.0
    private set
    var vOff = 0.0
        private set
    var distOff = 0.0
        private set
    override fun renderBackground(pGuiGraphics: GuiGraphics) {

    }
    //todo
    override fun doInit() {
        cameraOp = {
            it.move(distOff,vOff,hOff)
        }
    }
    private var moveFactor = DEFAULT_MOVE_FACTOR
    override fun doTick() {
        if(mc pressingKey InputConstants.KEY_LCONTROL)
            moveFactor=1.0
        else if(mc pressingKey InputConstants.KEY_LSHIFT)
            moveFactor=0.05
        else
            moveFactor = DEFAULT_MOVE_FACTOR
        if(mc pressingKey InputConstants.KEY_A)
            hOff+=moveFactor
        if(mc pressingKey InputConstants.KEY_D)
            hOff-=moveFactor
        if(mc pressingKey InputConstants.KEY_W)
            vOff+=moveFactor
        if(mc pressingKey InputConstants.KEY_S)
            vOff-=moveFactor

    }
    override fun doRender(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        guiGraphics.drawText("WASD前后左右移动")
        guiGraphics.drawText("P投影模式")
        guiGraphics.drawText("鼠标滚轮变焦")
        guiGraphics.drawText("鼠标移动改变视角")
    }
    override fun onClose() {
        mc go null
        cameraOp = {}
    }
}