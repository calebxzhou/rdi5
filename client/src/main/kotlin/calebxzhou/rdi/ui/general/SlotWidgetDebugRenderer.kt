package calebxzhou.rdi.ui.general

import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.mc.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen

//调试用
object SlotWidgetDebugRenderer {
    fun render(guiGraphics: GuiGraphics, screen: Screen) {
        //打开f3+h才开始渲染
        if(!(mc.options.advancedItemTooltips)) return
        if(screen is AbstractContainerScreen<*>){
            screen.menu.slots.forEachIndexed { index,slot->
                val offsetX = screen.guiLeft
                val offsetY = screen.guiTop
                guiGraphics.drawString(Font,"s${slot.index}",slot.x+offsetX+5,slot.y+offsetY+5,0x00ff00)
            }
        }
        screen.renderables.forEachIndexed { index, widget->
            if(widget is AbstractWidget){
                guiGraphics.drawString(Font,"w${index}",widget.x+5,widget.y+5,0xffff00)
            }
        }
    }

}