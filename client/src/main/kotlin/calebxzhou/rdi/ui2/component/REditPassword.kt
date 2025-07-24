package calebxzhou.rdi.ui2.component

import calebxzhou.rdi.ui2.PARENT
import calebxzhou.rdi.ui2.SELF
import calebxzhou.rdi.ui2.button
import calebxzhou.rdi.ui2.editText
import calebxzhou.rdi.ui2.frameLayoutParam
import calebxzhou.rdi.ui2.linearLayoutParam
import calebxzhou.rdi.ui2.paddingDp
import icyllis.modernui.core.Context
import icyllis.modernui.text.method.PasswordTransformationMethod
import icyllis.modernui.view.Gravity
import icyllis.modernui.widget.FrameLayout

class REditPassword(
    context: Context,
    val msg: String = "",
    val width_: Float = 200f,
) : FrameLayout(context) {
    private val editText = editText(context, msg, width_) {
        layoutParams = frameLayoutParam(
            dp(width_),
            SELF  // Fixed height instead of SELF
        ) {
            gravity = Gravity.CENTER
        }
    }
    private var isPasswordVisible = false

    init {
        layoutParams = linearLayoutParam(
            dp(width_),
            SELF  // Fixed height instead of SELF
        ) {
            bottomMargin = dp(16f)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        addView(editText)
        editText.setTransformationMethod(PasswordTransformationMethod.getInstance())

        // Add eye toggle button
        button(context) {
            text = "👁"
            background = null
            layoutParams = frameLayoutParam(dp(32f), PARENT) {  // Fixed height instead of PARENT
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
            }
            setOnClickListener {
                isPasswordVisible = !isPasswordVisible
                editText.setTransformationMethod(
                    if (isPasswordVisible) null
                    else PasswordTransformationMethod.getInstance()
                )
            }
        }.also { addView(it) }

        // Add right padding to editText to prevent text behind the button
        editText.paddingDp(8, 8, 40, 8)
    }

    var text
        get() = editText.text
        set(value) = editText.setText(value)
}