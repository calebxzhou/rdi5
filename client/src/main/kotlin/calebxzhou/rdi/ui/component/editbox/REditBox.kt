package calebxzhou.rdi.ui.component.editbox

import calebxzhou.rdi.mixin.AEditBox
import calebxzhou.rdi.ui.Font
import calebxzhou.rdi.ui.mcUIScale
import net.minecraft.client.gui.components.EditBox
import net.minecraft.network.chat.Component
import net.minecraft.util.StringUtil


open class REditBox(
     val length: Int = 16,
     val label: String = "",
     x: Int=0,
     y: Int=0
) : EditBox(Font, x, y, 20, 12, Component.literal(label)){
    var nullable = false
    var numberOnly = false
    //验证失败则返回错误原因 成功返回null
    var invalid : (REditBox) -> String? = { box ->
        if(box.value.isNullOrBlank() && !nullable)
            "未填写${box.label}"
        else
            null
    }
    init {
        setHint(Component.literal(label))
        setMaxLength(length)
        width = length * mcUIScale.toInt()
    }
    fun validate() = invalid(this)
    override fun charTyped(codePoint: Char, modifiers: Int): Boolean {

        if (!this.canConsumeInput()) {
            return false
        } else if (StringUtil.isAllowedChatCharacter(codePoint)) {
            if (this.isEditable) {
                if (numberOnly) {
                    if (codePoint.isDigit()) {
                        this.insertText(codePoint.toString())
                    }
                } else {
                    this.insertText(codePoint.toString())
                }
            }

            return true
        } else {
            return false
        }
    }

}