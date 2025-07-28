package calebxzhou.rdi.ui2.component

import calebxzhou.rdi.ui2.*
import icyllis.modernui.core.Context
import icyllis.modernui.text.method.PasswordTransformationMethod
import icyllis.modernui.view.Gravity
import icyllis.modernui.widget.Button
import icyllis.modernui.widget.FrameLayout

class REditPassword(
    context: Context,
    val msg: String = "",
    val width_: Float = 200f,
) : FrameLayout(context) {
    private val editText = editText(msg, width_) {
        layoutParams = frameLayoutParam(
            dp(width_),
            SELF  // Fixed height instead of SELF
        ) {
            gravity = Gravity.CENTER
            setTransformationMethod(PasswordTransformationMethod.getInstance())
        }
        paddingDp(8, 8, 40, 8)
    }
    private var isPasswordVisible = false

    init {
        // Add right padding to editText to prevent text behind the button
        layoutParams = linearLayoutParam(
            dp(width_),
            SELF  // Fixed height instead of SELF
        ) {
            bottomMargin = dp(16f)
            gravity = Gravity.CENTER_HORIZONTAL
        }


        // Add eye toggle button
        Button(context).apply {
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


    }

    var text
        get() = editText.text
        set(value) = editText.setText(value)
}