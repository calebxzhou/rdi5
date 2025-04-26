package calebxzhou.rdi.ui.screen

import calebxzhou.rdi.common.WHITE
import calebxzhou.rdi.ui.Font
import calebxzhou.rdi.ui.UiHeight
import calebxzhou.rdi.ui.UiWidth
import calebxzhou.rdi.ui.component.RScreen
import calebxzhou.rdi.ui.general.Icons
import calebxzhou.rdi.ui.matrixOp
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.isMcStarted
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.mcAsset
import calebxzhou.rdi.util.pressingKey
import com.mojang.blaze3d.platform.InputConstants
import com.mojang.math.Axis
import net.minecraft.Util
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation

class LoadingScreen() : RScreen("请求中") {
    var startX = 0
    var startY = UiHeight / 2 - 50
    override var showTitle = false
    override var clearColor = false
    override var closeable = true
    companion object {
        val SIZE=32
        val ICON_RL = Icons["loading"]
        val BG_RL = mcAsset("textures/block/stone.png")
        fun renderLoadingIcon(guiGraphics: GuiGraphics, x: Int = UiWidth/2, y: Int=UiHeight/2, size: Int=SIZE){

            val millis = Util.getMillis() % 1000L // Get the current time in milliseconds and take the remainder when divided by 1000
            val angle = (millis / 1000.0) * 360 // Map the time to a range of 0 to 360 degrees

            guiGraphics.matrixOp {
                // Translate to center the logo scale(scaledF, scaledF, scaledF)
                translate(x.toFloat(), y.toFloat() , 1f)
                mulPose(Axis.ZP.rotationDegrees(angle.toFloat()))
                // Adjust translation to account for scaling
                translate(-16f, -16f, 0f)
                guiGraphics.blit(ICON_RL, 0, 0, 0f, 0f, SIZE, SIZE, SIZE, SIZE)
            }
        }
        fun show(){
            if (isMcStarted) {
                mc.screen?.let { screen ->
                    mc go LoadingScreen()
                }
            }
        }
        fun close(){
            if (isMcStarted) {
                mc.screen?.let { screen ->
                    if (screen is LoadingScreen)
                        screen.onClose()
                }
            }
        }
    }

    override fun init() {

        super.init()
    }
    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
       // prevScreen.render(guiGraphics,mouseX,mouseY,partialTick)
        renderLoadingIcon(guiGraphics,mouseX,mouseY,width)
        guiGraphics.blit(
            BG_RL, startX,
            startY, 0, 0.0F, 0.0F, width, 32, 32, 32
        )
        guiGraphics.drawCenteredString(Font, "载入中，请稍候...", UiWidth / 2, startY + 12, WHITE)
        super.render(guiGraphics, mouseX, mouseY, partialTick)
    }


    override fun tick() {
        if(mc pressingKey InputConstants.KEY_ESCAPE){
            mc.popGuiLayer()
        }
        super.tick()
    }
}