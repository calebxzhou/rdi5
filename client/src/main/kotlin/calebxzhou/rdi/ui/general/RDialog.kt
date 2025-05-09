package calebxzhou.rdi.ui.general

import calebxzhou.rdi.common.WHITE
import calebxzhou.rdi.ui.FONT
import calebxzhou.rdi.ui.RMessageLevel
import calebxzhou.rdi.ui.UiHeight
import calebxzhou.rdi.ui.UiWidth
import calebxzhou.rdi.ui.component.RScreen
import calebxzhou.rdi.ui.layout.gridLayout
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.mcAsset
import calebxzhou.rdi.util.mcComp
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.MultiLineTextWidget
import net.minecraft.network.chat.MutableComponent
import org.lwjgl.util.tinyfd.TinyFileDialogs


/**
 * Created on 2024-06-23,22:05.
 */
fun alertOs(msg: String) {
    TinyFileDialogs.tinyfd_messageBox("RDI提示", msg, "ok", "info", true)
}
fun alertOsErr(msg: String) {
    TinyFileDialogs.tinyfd_messageBox("RDI错误", msg, "ok", "error", true)
}
fun notifyOs(msg: String){
    TinyFileDialogs.tinyfd_notifyPopup("RDI提示", msg,"info" )
}
fun alertErr(msg: String) {
    dialog(msg.mcComp, msglvl = RMessageLevel.ERR)
}

fun alert(msg: String) {
    dialog(msg.mcComp)
}
fun confirm(msg:String, onYes: () -> Unit){
    dialog(msg.mcComp, RDialogType.CONFIRM, onYes=onYes)
}
fun dialog(
    msgBuilder: MutableComponent,
    diagType: RDialogType = RDialogType.ALERT,
    msglvl: RMessageLevel = RMessageLevel.INFO,
    yesText: String = if (diagType == RDialogType.ALERT) "明白" else "是",
    noText: String = "否",
    onYes: () -> Unit = {},
    onNo: () -> Unit = {},
) {
    val title = 
        when (msglvl) {
            RMessageLevel.INFO -> "提示"
            RMessageLevel.ERR -> "错误"
            RMessageLevel.OK -> "成功"
            RMessageLevel.WARN -> "警告"
        }.mcComp

    mc go RDialog(title, diagType, msglvl, msgBuilder, yesText, noText, onYes, onNo)
}

enum class RDialogType {
    CONFIRM, ALERT
}

class RDialog(
    override var title: MutableComponent,
    val diagType: RDialogType,
    val msglvl: RMessageLevel,
    val msg: MutableComponent,
    val yesText: String,
    val noText: String,
    val onYes: () -> Unit,
    val onNo: () -> Unit,
) : RScreen("提示") {
    companion object {
        val BG_RL = mcAsset("textures/block/stone.png")
    }

    override var closeable = true
    override var showTitle = false
    lateinit var msgWidget: MultiLineTextWidget
    var startX = UiWidth / 2 - 100
    var startY = UiHeight / 2 - 50
    var width = 200
    var height = 100
    val iconSize = 14
    val icon = when (msglvl) {
        RMessageLevel.INFO -> Icons["info"]
        RMessageLevel.ERR -> Icons["error"]
        RMessageLevel.OK -> Icons["success"]
        RMessageLevel.WARN -> Icons["warning"]
    }/*
    override */

    override fun init() {
        gridLayout(this, y = startY+height, hAlign = HAlign.CENTER) {
            button("success", text = yesText) {
                mc.popGuiLayer()
                onYes()
            }
            if (diagType == RDialogType.CONFIRM) {
                button("error", text = noText) {
                    mc.popGuiLayer()
                    onNo()
                }
            }
        }

        msgWidget = MultiLineTextWidget(startX + 3, startY + 18, msg, FONT).apply { setMaxWidth(this@RDialog.width) }
        val msgWidgetWidth = msgWidget.width
        val msgWidgetHeight = msgWidget.height
        val centeredX = startX + (width - msgWidgetWidth) / 2
        val centeredY = startY + (height - msgWidgetHeight) / 2  // Center vertically within the content area
        msgWidget.setPosition(centeredX, centeredY)
        super.init()

    }

    override fun onPressEnterKey() {
        onYes()
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {


        guiGraphics.blit(
            BG_RL, startX,
            startY, 0, 0.0F, 0.0F, width, height + 16, 32, 32
        );
        guiGraphics.fill(startX, startY + 16, startX + width, startY + height + 16, 0xAA000000.toInt())

        guiGraphics.blit(icon, startX + 1, startY + 1, 0f, 0f, iconSize, iconSize, iconSize, iconSize)
        guiGraphics.drawString(FONT, title, startX + iconSize + 2, startY + 4, WHITE)
        msgWidget.render(guiGraphics, mouseX, mouseY, partialTick)


        super.render(guiGraphics, mouseX, mouseY, partialTick)


    }


}
