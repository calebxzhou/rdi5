package calebxzhou.rdi.ui.screen

import calebxzhou.rdi.service.ChatService
import calebxzhou.rdi.ui.UiHeight
import calebxzhou.rdi.ui.component.button.RButton
import calebxzhou.rdi.util.mcComp
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.ChatScreen

class RChatScreen(val defaultText: String) : ChatScreen(defaultText) {
    val btn = RButton(if (ChatService.chatMode == 1) "岛内" else "全服") {

        if (ChatService.chatMode == 1) {
            ChatService.chatMode = 0
            it.message = "全服".mcComp
        } else {
            ChatService.chatMode = 1
            it.message = "岛内".mcComp
        }

    }.apply {
        x= 0
        y=UiHeight - 30
        height = 12
    }
    override fun init() {
        super.init()
    }
    override fun tick() {

        super.tick()

    }

    override fun mouseClicked(pMouseX: Double, pMouseY: Double, pButton: Int): Boolean {
        btn.mouseClicked(pMouseX,pMouseY,pButton)
        return super.mouseClicked(pMouseX, pMouseY, pButton)
    }

    override fun render(pGuiGraphics: GuiGraphics, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick)
        btn.render(pGuiGraphics,pMouseX,pMouseY,pPartialTick)
    }
}