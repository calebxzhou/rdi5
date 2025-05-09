package calebxzhou.rdi.ui

import calebxzhou.rdi.util.mcComp

/**
 * calebxzhou @ 2025-04-24 10:23
 */
import calebxzhou.rdi.common.WHITE
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.rdiAsset
import com.mojang.blaze3d.platform.Lighting
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.Util
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import org.joml.Matrix4f

val FONT
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
    get() = FONT.lineHeight
val Component.width
    get() = FONT.width(this)
val String.width
    get() = FONT.width(this)
val CenterY
    get() = (UiHeight-FONT.lineHeight)/2
val CenterX
    get() = {text:String? -> (UiWidth-(text?.width?:0))/2}
val MouseX
    get() = (mc.mouseHandler.xpos() * UiWidth / ScreenWidth).toInt()
val MouseY
    get() = (mc.mouseHandler.ypos() * UiHeight / ScreenHeight).toInt()
val MouseLeftClicked
    get() = mc.mouseHandler.isLeftPressed
fun AbstractWidget.justify() {
    x = UiWidth / 2 - width / 2
}
fun mouseInside(x1:Int,y1:Int,x2:Int,y2:Int) = MouseX in (x1..x2) && MouseY in (y1..y2)

//全屏填充
infix fun GuiGraphics.fill(color: Int) {
    fill(0, 0, UiWidth, UiHeight, color)
}
fun String.openAsUri(){
    Util.getPlatform().openUri(this)
}
fun GuiGraphics.drawImage(
    texture: String,
    width: Int,
    height: Int,
    x: Int=0,
    y:Int=0,

    ){
    blit(rdiAsset(texture), x,y,0f,0f,width,height,width, height)
}
fun GuiGraphics.drawText(
    text: String="",
    comp: Component = text.mcComp,
    x: Int = 0,
    y: Int= 0,
    color: Int = WHITE,
)=drawString(FONT,comp,x,y,color,true)


fun GuiGraphics.renderItemStack(x: Int = 0, y: Int = 0, itemStack: ItemStack, width: Int = 16, height: Int = 16) {
    matrixOp {
        val bakedmodel: BakedModel = mc.itemRenderer.getModel(itemStack, null, null, 1)
        //水平翻转
        translate(x + width / 2, y + height / 3, 5)
        scale(width.toFloat(), height.toFloat(), 1f)
        mulPose(Matrix4f().scaling(1.0f, -1.0f, 1.0f))
        Lighting.setupForFlatItems()
        mc.itemRenderer.render(
            itemStack, ItemDisplayContext.GUI, false,
            this,
            bufferSource(), 15728880, OverlayTexture.NO_OVERLAY, bakedmodel
        )
        flush()
        Lighting.setupFor3DItems()
    }
}

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
