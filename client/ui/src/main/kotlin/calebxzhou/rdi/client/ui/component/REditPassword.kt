package calebxzhou.rdi.client.ui.component

import calebxzhou.rdi.client.ui.*
import icyllis.modernui.core.Context
import icyllis.modernui.graphics.Canvas
import icyllis.modernui.graphics.Paint
import icyllis.modernui.graphics.drawable.Drawable
import icyllis.modernui.text.method.PasswordTransformationMethod
import icyllis.modernui.view.Gravity
import icyllis.modernui.widget.Button
import icyllis.modernui.widget.EditText
import icyllis.modernui.widget.FrameLayout

class REditPassword(
    context: Context,
    val msg: String = "",
    val width_: Float = 200f,
) : FrameLayout(context) {
    private val editText = EditText(context).apply {
        hint = msg
        // Let container control width; make EditText match parent minus padding
        layoutParams = frameLayoutParam(PARENT, SELF) {
            gravity = Gravity.CENTER_VERTICAL or Gravity.START
        }
        // MD3-ish paddings
        paddingDp(12, 8, 40, 8)
        // Hide default background for a custom filled container look
        background = null
        // Start hidden characters
        setTransformationMethod(PasswordTransformationMethod.getInstance())
    }
    private var isPasswordVisible = false

    init {
        // Container (this) controls width and draws background like RTextField
        layoutParams = linearLayoutParam(dp(width_), SELF) {
            bottomMargin = dp(12f)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        background = object : Drawable() {
            override fun draw(canvas: Canvas) {
                val r = dp(12f).toFloat()
                val fill = Paint.obtain()
                val line = Paint.obtain()
                // Filled box bg
                fill.setRGBA(245, 245, 245, 255)
                fill.style = Paint.Style.FILL.ordinal
                canvas.drawRoundRect(bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat(), r, fill)
                // Bottom line
                line.setRGBA(180, 180, 180, 255)
                line.style = Paint.Style.FILL.ordinal
                val h = dp(1f).toFloat()
                canvas.drawRect(bounds.left.toFloat()+dp(12f), bounds.bottom - h, bounds.right.toFloat()-dp(12f), bounds.bottom.toFloat(), line)
                fill.recycle(); line.recycle()
            }
        }


        // Add eye toggle button
        val eye = Button(context).apply {
            text = "👁"
            background = null
            layoutParams = frameLayoutParam(dp(32f), PARENT) {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
            }
            setOnClickListener {
                isPasswordVisible = !isPasswordVisible
                editText.setTransformationMethod(
                    if (isPasswordVisible) null
                    else PasswordTransformationMethod.getInstance()
                )
            }
        }
        // Order: edit first, then eye so eye is on top
        addView(editText)
        addView(eye)

    }

    var text
        get() = editText.text
        set(value) = editText.setText(value)
}