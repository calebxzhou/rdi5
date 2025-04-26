package calebxzhou.rdi.ui.component.editbox

import calebxzhou.rdi.ui.Font
import net.minecraft.Util
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation

class RPasswordEditBox(label: String) : REditBox(16,label) {

    var passwordVisible = false
    init {
        setHint(Component.literal(label))
    }
    
    override fun renderWidget(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        if (!isVisible) return
        // 鼠标框内显示密码
        passwordVisible = isFocused
        if (isBordered) {
            val borderSprite: ResourceLocation = SPRITES.get(isActive, isFocused)
            guiGraphics.blitSprite(borderSprite, x, y, width, height)
        }
        //输入的字符全都换成星号
        val value = if(!passwordVisible) value.replace(Regex("."), "*") else value
        val currentTextColor = if (isEditable) textColor else textColorUneditable
        val cursorOffset = cursorPos - displayPos
        val visibleText = Font.plainSubstrByWidth(value.substring(displayPos), getInnerWidth())
        val isCursorVisible = cursorOffset >= 0 && cursorOffset <= visibleText.length
        val shouldDrawCursor = isFocused() && (Util.getMillis() - focusedTime) / 300L % 2L == 0L && isCursorVisible
        val textX = if (isBordered) x + 4 else x
        val textY = if (isBordered) y + (height - 8) / 2 else y
        var currentX = textX
        val highlightOffset = (highlightPos - displayPos).coerceIn(0, visibleText.length)

        if (visibleText.isNotEmpty()) {
            val textBeforeCursor = if (isCursorVisible) visibleText.substring(0, cursorOffset) else visibleText
            currentX = guiGraphics.drawString(Font, formatter.apply(textBeforeCursor, displayPos), textX, textY, currentTextColor, textShadow)
        }

        val useBlockCursor = cursorPos < value.length || value.length >= maxLength
        var cursorX = currentX
        if (!isCursorVisible) {
            cursorX = if (cursorOffset > 0) textX + width else textX
        } else if (useBlockCursor) {
            cursorX = currentX - 1
            currentX--
        }

        if (visibleText.isNotEmpty() && isCursorVisible && cursorOffset < visibleText.length) {
            guiGraphics.drawString(Font, formatter.apply(visibleText.substring(cursorOffset), cursorPos), currentX, textY, currentTextColor, textShadow)
        }

        if (hint != null && visibleText.isEmpty() && !isFocused) {
            guiGraphics.drawString(Font, hint, currentX, textY, currentTextColor, textShadow)
        }

        if (shouldDrawCursor) {
            if (useBlockCursor) {
                guiGraphics.fill(RenderType.guiOverlay(), cursorX, textY - 1, cursorX + 1, textY + 1 + 9, -3092272)
            } else {
                guiGraphics.drawString(Font, "_", cursorX, textY, currentTextColor, textShadow)
            }
        }

        if (highlightOffset != cursorOffset) {
            val startOffset = minOf(cursorOffset, highlightOffset)
            val endOffset = maxOf(cursorOffset, highlightOffset)
            val startX = textX + Font.width(visibleText.substring(0, startOffset))
            val endX = textX + Font.width(visibleText.substring(0, endOffset))
            renderHighlight(guiGraphics, startX, textY - 1, endX - 1, textY + 1 + 9)
        }
    }


}