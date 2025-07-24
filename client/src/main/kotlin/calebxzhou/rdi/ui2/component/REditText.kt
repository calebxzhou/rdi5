package calebxzhou.rdi.ui2.component

import calebxzhou.rdi.ui2.BG_GRAY_BORDER
import calebxzhou.rdi.ui2.padding8dp
import icyllis.modernui.core.Context
import icyllis.modernui.view.Gravity
import icyllis.modernui.widget.EditText
import icyllis.modernui.widget.LinearLayout

class REditText(
    context: Context,
    val msg: String = "",
    val width: Float = 200f,
) : EditText(context) {
    init {
        hint = msg
        background = BG_GRAY_BORDER
        padding8dp()
        layoutParams = LinearLayout.LayoutParams(
            dp(width),
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(16f)
            gravity = Gravity.CENTER_HORIZONTAL
        }
    }
}