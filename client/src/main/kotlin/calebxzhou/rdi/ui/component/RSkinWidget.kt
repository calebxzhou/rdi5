package calebxzhou.rdi.ui.component

import calebxzhou.rdi.common.WHITE
import calebxzhou.rdi.ui.general.Icons
import calebxzhou.rdi.ui.general.Icons.draw
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.mc.Font
import calebxzhou.rdi.util.mc.matrixOp
import kotlinx.serialization.Serializable
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.PlayerFaceRenderer
import net.minecraft.client.renderer.texture.HttpTexture
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.resources.ResourceLocation
import java.io.File

@Serializable
data class SkinData(
    val tid: Int,
    val name: String,
    val type: String,
    val uploader: Int,
    val public: Boolean,
    val likes: Int,
    val nickname: String
){
    val isCape = type == "cape"
    val isSlim = type == "alex"
}

class RSkinWidget(
    val data: SkinData, x: Int = 0,
    y: Int = 0, width: Int, height: Int,val click: (RWidget) -> Unit = {}
) : RWidget(x, y, width, height, null), AutoCloseable {
    val RL = ResourceLocation("rdi", "little_skins/${data.tid}")
    var loaded = false
    var fontScale = 0.6f
    val displayName = data.name.replace(
        Regex("[^\\p{L}\\p{N}\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}\\p{InCJKUnifiedIdeographs}]"),
        ""
    ).run { truncateStringWithEllipsis(this,width) }
    init {
        val txt = mc.textureManager.getTexture(RL, MissingTextureAtlasSprite.getTexture())
        //没有皮肤就下载，有就不下载
        if (txt == MissingTextureAtlasSprite.getTexture()) {
            mc.textureManager.register(
                RL, HttpTexture(
                    File("cache/little_skins/${data.tid}.png"),
                    "https://littleskin.cn/preview/${data.tid}?height=150&png",
                    Icons["loading"],
                    false
                ) {
                    //logger.info("皮肤载入：$RL x$x y$y w$width h$height")
                    loaded = true
                })
        }else{
            loaded=true
        }
    }
    //过长截断省略号
    fun truncateStringWithEllipsis(text: String, maxWidth: Int): String {
        var truncatedText = text
        while (Font.width(truncatedText)*fontScale > maxWidth && truncatedText.isNotEmpty()) {
            truncatedText = truncatedText.dropLast(1)
        }
        return if (Font.width("$truncatedText...")*fontScale >= maxWidth) {
            "$truncatedText..."
        } else {
            truncatedText
        }
    }

    override fun onClick(mouseX: Double, mouseY: Double) {
        this.click(this)
    }
    override fun doRenderWidget(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float
    ) {
        if (loaded) {
            guiGraphics.blit(RL, x, y, 0f, 0f, width, height, width, height)
        }

        val heartX = x + width / 3 * 2
        guiGraphics.matrixOp {
            //缩放
            scale(fontScale, fontScale, 1f)
            translate(x.toDouble() / fontScale, (y.toDouble() + height) / fontScale, 1.0)
            guiGraphics.drawString(Font, displayName, 0, 0, WHITE)
            Icons["heart"].draw(guiGraphics, 0, 9)
            guiGraphics.drawString(Font, data.likes.toString(), 12, 9, WHITE)
            val atlas = if(data.type=="steve"||data.type=="alex") ResourceLocation.parse("textures/entity/player/slim/${data.type}.png") else Icons["clothes"]
            PlayerFaceRenderer.draw(
                guiGraphics,
                atlas,
                35,
                10,
                8
            )
            guiGraphics.drawString(Font, data.type, 45, 10, WHITE)
        }
    }

    override fun close() {
        //mc.textureManager.release(RL)
    }

}