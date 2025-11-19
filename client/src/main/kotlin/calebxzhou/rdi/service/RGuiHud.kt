package calebxzhou.rdi.service

import calebxzhou.rdi.util.Font
import calebxzhou.rdi.util.UiWidth
import calebxzhou.rdi.util.mcComp
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics

object RGuiHud {


    @JvmStatic
    fun renderSelectedItemEnglishName(
        guiGraphics: GuiGraphics,
        langKey: String,
        cnX: Int,
        cnY: Int,
        alpha: Int
    ) {
        val color = 0xaaaaaa + (alpha shl 24)
        val comp = EnglishStorage[langKey].mcComp.withStyle(ChatFormatting.ITALIC)
        guiGraphics.drawString(Font, comp, (UiWidth - Font.width(comp)) / 2, cnY + 9, color)
    }









}