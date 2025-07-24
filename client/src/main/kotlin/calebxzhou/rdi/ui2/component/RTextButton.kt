package calebxzhou.rdi.ui2.component

import calebxzhou.rdi.ui2.BG_GRAY_BORDER
import calebxzhou.rdi.ui2.SELF
import calebxzhou.rdi.ui2.linearLayoutParam
import calebxzhou.rdi.ui2.paddingDp
import icyllis.modernui.core.Context
import icyllis.modernui.view.Gravity
import icyllis.modernui.widget.Button

class RTextButton(
    context: Context,
    val msg: String,
    val onClick: () -> Unit = {},
) : Button(context) {
    init {
        text = msg
        background = BG_GRAY_BORDER
        paddingDp(16,8,16,8)
        layoutParams = linearLayoutParam(SELF,SELF) {
            gravity = Gravity.CENTER_HORIZONTAL
        }
        setOnClickListener {
            onClick()
        }
    }
}