package calebxzhou.rdi.service

import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.util.*
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.PlayerFaceRenderer

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