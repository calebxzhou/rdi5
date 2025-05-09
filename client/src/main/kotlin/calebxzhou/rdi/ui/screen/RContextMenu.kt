package calebxzhou.rdi.ui.screen

import calebxzhou.rdi.ui.MouseLeftClicked
import calebxzhou.rdi.ui.MouseX
import calebxzhou.rdi.ui.MouseY
import calebxzhou.rdi.ui.UiHeight
import calebxzhou.rdi.ui.component.RScreen
import calebxzhou.rdi.ui.component.RTooltip
import calebxzhou.rdi.ui.layout.RLinearLayout
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.mc
import net.minecraft.client.gui.GuiGraphics

fun contextMenu( layoutBuilder: RLinearLayout.() -> Unit) = mc go RContextMenu(layoutBuilder)
class RContextMenu(
    val layoutBuilder: RLinearLayout.() -> Unit
): RScreen("选项菜单") {
    override var showCloseButton=false
    override var showTitle=false
    var startX = MouseX.toInt()
    var startY = MouseY.toInt()
    lateinit var layout: RLinearLayout
    override fun doInit() {
        layout = RLinearLayout(this).apply {
            startX=this@RContextMenu.startX
            startY=this@RContextMenu.startY
            horizontal=false
            center=false
            marginY=5
            layoutBuilder()
        }.build()
        if(layout.totalHeight + startY > UiHeight){
            layout.startY -= startY+layout.totalHeight-UiHeight
            this.startY = layout.startY
            layout.updatePosition()
        }
    }
    override fun doRender(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        RTooltip.renderBackground(guiGraphics,startX,startY, (layout.totalWidth),layout.totalHeight)
    }

    override fun doTick() {
        //点击任何区域 关闭菜单
        if(MouseLeftClicked)/* && !mouseInside(x,y,x+layout.totalWidth,y+layout.totalHeight)){*/
            onClose()

    }

    override fun mouseClicked(pMouseX: Double, pMouseY: Double, pButton: Int): Boolean {
        if(tickCounter>5)
        onClose()
        return super.mouseClicked(pMouseX, pMouseY, pButton)
    }


}