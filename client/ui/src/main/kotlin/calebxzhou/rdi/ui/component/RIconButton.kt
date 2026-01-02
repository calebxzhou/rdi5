package calebxzhou.rdi.ui.component

import calebxzhou.rdi.ui.iconDrawable
import calebxzhou.rdi.ui.paddingDp
import icyllis.modernui.core.Context

class RIconButton(
    context: Context,
    val icon: String,
    val msg: String="",
    onClick: (RButton) -> Unit = {},
) : RButton(context,onClick=onClick){
    init {
        text = msg
        paddingDp(16,8,16,8)

        // Set the icon as a compound drawable
        val drawable = iconDrawable(icon)
        drawable.setBounds(0, 0, dp(24f), dp(24f))
        setCompoundDrawablePadding(dp(8f))  // Add 8dp spacing between icon and text
        setCompoundDrawables(drawable, null, null, null)


    }

}