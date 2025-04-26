package calebxzhou.rdi.ui.general

import calebxzhou.rdi.common.WHITE
import calebxzhou.rdi.ui.component.RScreen
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.mc.Font
import calebxzhou.rdi.util.mc.UiHeight
import calebxzhou.rdi.util.pressingKey
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen

data class ROption(val text:String,val enabled: Boolean = true,val hoverText: String?=null,val action: (ROptionScreen) -> Unit)
fun optionScreen(title: String = "请选择", init: ROptionScreen.() -> Unit): RScreen {
    return ROptionScreen( title).apply(init).mcScreen
}

class ROptionScreen(
    val title: String,
) {
    val options = arrayListOf<ROption>()
    operator fun plusAssign(option: ROption){
        options += option
    }
    infix fun String.to(action: (ROptionScreen) -> Unit) {
        options += ROption(this, action = action)
    }
    infix fun String.to(screen: Screen) {
        options += ROption(this){mc go  screen}
    }

    val mcScreen
        get() = object : RScreen(title) {
            override var showTitle = false
            val startY = UiHeight / 2 - options.size * 10
            var nowY = startY
            override fun init() {
                nowY = startY
                options.onEachIndexed { index, option ->
                    //TODO 重写 只需要像dialog一样
                    /*val btn = RButton("${index + 1}. ${option.text}", 0, nowY) { option.action(this@ROptionScreen) }
                    btn.width = (option.text.width * 1.8).toInt()
                    btn.x = width/2-btn.width/2
                    btn.tooltip = option.hoverText?.let { Tooltip.create(it.mcComp) }
                    btn.active = option.enabled
                    addWidget(btn)
                    nowY += btn.height*/
                }
                super.init()
            }

            override fun doRender(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
                guiGraphics.drawCenteredString(Font,this@ROptionScreen.title,width/2,startY-20, WHITE)
            }
            override fun tick() {

                if (mc pressingKey InputConstants.KEY_1) {
                    options.getOrNull(0)?.action?.invoke(this@ROptionScreen)
                }
                if (mc pressingKey InputConstants.KEY_2) {
                    options.getOrNull(1)?.action?.invoke(this@ROptionScreen)
                }
                if (mc pressingKey InputConstants.KEY_3) {
                    options.getOrNull(2)?.action?.invoke(this@ROptionScreen)
                }
                if (mc pressingKey InputConstants.KEY_4) {
                    options.getOrNull(3)?.action?.invoke(this@ROptionScreen)
                }
                if (mc pressingKey InputConstants.KEY_5) {
                    options.getOrNull(4)?.action?.invoke(this@ROptionScreen)
                }
                if (mc pressingKey InputConstants.KEY_6) {
                    options.getOrNull(5)?.action?.invoke(this@ROptionScreen)
                }
                if (mc pressingKey InputConstants.KEY_7) {
                    options.getOrNull(6)?.action?.invoke(this@ROptionScreen)
                }
                if (mc pressingKey InputConstants.KEY_8) {
                    options.getOrNull(7)?.action?.invoke(this@ROptionScreen)
                }
                if (mc pressingKey InputConstants.KEY_9) {
                    options.getOrNull(8)?.action?.invoke(this@ROptionScreen)
                }
                if (mc pressingKey InputConstants.KEY_0) {
                    options.getOrNull(9)?.action?.invoke(this@ROptionScreen)
                }
            }
        }
}
