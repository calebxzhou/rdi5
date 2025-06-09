package calebxzhou.rdi.ui.screen

import calebxzhou.rdi.hardware.CPU
import calebxzhou.rdi.hardware.SPECS
import calebxzhou.rdi.hardware.GPU
import calebxzhou.rdi.hardware.MEM
import calebxzhou.rdi.hardware.OS
import calebxzhou.rdi.ui.screen.RScreen
import calebxzhou.rdi.ui.drawImage
import calebxzhou.rdi.ui.text.richText
import net.minecraft.client.gui.GuiGraphics

class AboutScreen : RScreen("关于") {
    val rt = richText(10,20){
        text("如果喜欢我开发的服务器，希望能小小赞助一下，你的支持就是我开发的动力。")
        ret()
        text("感谢大家的支持，没有你们RDI就无法走到今天..")
        ret()
        icon("server")
        text("服务器：科洛 wuhudsm66")
        ret()
        icon("plugin")
        text("建议：ForiLuSa 谋士兔 Caragan J4ckTh3R1pper Juliiee ouhehe0 xiao_yan_t")
        ret()
        icon("test")
        text("测试：wuhudsm66 谋士兔 狗查 Caragan Joon Alive 木果小奶喵")
        ret()
        icon("qq")
        text("QQ群：1095925708")
        ret()
        icon("github")
        text("GitHub：calebxzhou/rdi4")
        ret()
        icon("os")
        text("OS：${OS}")
        ret()
        icon("cpu")
        text("CPU：${CPU}")
        ret()
        icon("gpu")
        text("GPU：${GPU}")
        ret()
        icon("ram")
        text("RAM: ${MEM}")
        ret()
        icon("disp")
        text("显示:")
        ret()
        SPECS.forEach {
            text(it)
            ret()
        }
        fill()
    }

    override fun doRender(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        rt.render(guiGraphics)

        guiGraphics.drawImage("textures/gui/sponsor.png",200,100,200,130)
    }
}