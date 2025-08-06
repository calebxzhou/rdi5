package calebxzhou.rdi.util

import calebxzhou.rdi.common.WHITE
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component

/**
 * calebxzhou @ 2025-08-05 15:28
 */
val Font
    get() = mc.font
val WindowHandle
    get() = mc.window.window
val UiWidth
    get() = mc.window.guiScaledWidth
val UiHeight
    get() = mc.window.guiScaledHeight
val mcWidth
    get() = mc.window.width
val mcHeight
    get() = mc.window.height
val ScreenWidth
    get() = mc.window.screenWidth
val ScreenHeight
    get() = mc.window.screenHeight
val mcUIScale
    get() = mc.window.guiScale
val LineHeight
    get() = Font.lineHeight
val Component.width
    get() = Font.width(this)
val String.width
    get() = Font.width(this)
val CenterY
    get() = (UiHeight- Font.lineHeight)/2
val CenterX
    get() = {text:String? -> (UiWidth-(text?.width?:0))/2}
val MouseX
    get() = (mc.mouseHandler.xpos() * UiWidth / ScreenWidth).toInt()
val MouseY
    get() = (mc.mouseHandler.ypos() * UiHeight / ScreenHeight).toInt()

fun GuiGraphics.matrixOp(handler: PoseStack.() -> Unit) {
    val stack = pose()
    stack.matrixOp(handler)
}

fun PoseStack.matrixOp(handler: PoseStack.() -> Unit) {
    pushPose()
    handler(this)
    popPose()
}

fun PoseStack.translate(x: Int, y: Int, z: Int) {
    translate(x.toFloat(), y.toFloat(), z.toFloat())
}
fun PoseStack.scale(factor:Double){
    scale(factor.toFloat(),factor.toFloat(),1f)
}
fun PoseStack.translate0() {
    translate(0,0,1)
}
//平移到原点,缩放,再平移回去
//字体缩小渲染用
fun PoseStack.tran0ScaleBack(x:Int,y:Int,scaleFactor: Double){
    translate0()
    scale(scaleFactor)
    translate(x/scaleFactor,y/scaleFactor,1.0)
}
fun GuiGraphics.drawText(
    text: String="",
    comp: Component = text.mcComp,
    x: Int = 0,
    y: Int= 0,
    color: Int = WHITE,
)=drawString(Font,comp,x,y,color,true)

