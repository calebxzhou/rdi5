package calebxzhou.rdi.ui.component.button

import calebxzhou.rdi.service.RAccountService
import calebxzhou.rdi.ui.FONT
import calebxzhou.rdi.util.isTextureReady
import calebxzhou.rdi.util.mcAsync
import calebxzhou.rdi.util.mcComp
import kotlinx.coroutines.launch
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.PlayerFaceRenderer
import net.minecraft.client.resources.DefaultPlayerSkin
import org.bson.types.ObjectId

class RPlayerHeadButton(
    val id: ObjectId,
    onClick: (Button) -> Unit={},
) : RButton(id.toString(), onClick) {
    init {
        width = ofWidth(DEFAULT_NAME)
        height = HEAD_SIZE
    }
    companion object {
        const val DEFAULT_NAME = "载入中..."
        const val HEAD_SIZE = 14
        const val gapHeadName = 5
        private fun ofWidth(name:String): Int {
            return HEAD_SIZE + gapHeadName + FONT.width(name)
        }
    }

    override var textComp = DEFAULT_NAME.mcComp
    var headSkin = DefaultPlayerSkin.getDefaultTexture()
    init {
        _root_ide_package_.calebxzhou.rdi.util.ioScope.launch {
            val info = RAccountService.retrievePlayerInfo(id)

          //  headSkin =info.cloth.skinLocation
            textComp = info.name.mcComp
            width = ofWidth(info.name)
        }
    }

    override fun renderWidget(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float
    ) {
        val skin = if(headSkin.isTextureReady) headSkin else DefaultPlayerSkin.getDefaultTexture()
        PlayerFaceRenderer.draw(
            guiGraphics,
            skin    ,
            x,
            y,
            HEAD_SIZE
        )
        guiGraphics.drawString(FONT, textComp, x + HEAD_SIZE + gapHeadName, y + 3, 0xffffff)
    }
}