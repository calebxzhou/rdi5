package calebxzhou.rdi.ui.screen

import calebxzhou.rdi.model.RServer
import calebxzhou.rdi.ui.component.RScreen
import calebxzhou.rdi.ui.layout.linearLayout
import calebxzhou.rdi.util.mc.drawImage
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import org.bson.types.ObjectId

class RSponsorScreen : RScreen("感谢你的赞助") {
    override fun doRender(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        guiGraphics.drawImage("textures/gui/sponsor.png",150,75,width/2-75,30)
    }

    override fun doInit() {
        linearLayout(this, init = {
            startY = height/2
            RServer.now?.let {
                head(ObjectId("67f00df2eb902e3abb139a02"), init = {
                    tooltip="2025-04-11 ￥100".mcTooltip
                })
            }
        })
        linearLayout(this) {

            text("2025-04-11 感谢ChenQu赞助100元".mcComp.withStyle(ChatFormatting.GOLD)){
              //  "https://afdian.com/a/rdimc".openAsUri()
            }
        }
    }
}