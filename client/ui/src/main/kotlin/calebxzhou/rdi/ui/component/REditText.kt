package calebxzhou.rdi.ui.component

import calebxzhou.rdi.ui.MaterialColor
import calebxzhou.rdi.ui.dp
import icyllis.modernui.R
import icyllis.modernui.core.Context
import icyllis.modernui.graphics.Canvas
import icyllis.modernui.graphics.Paint
import icyllis.modernui.graphics.drawable.Drawable
import icyllis.modernui.view.Gravity
import icyllis.modernui.view.MotionEvent
import icyllis.modernui.view.View
import icyllis.modernui.widget.EditText
import icyllis.modernui.widget.LinearLayout

class REditText(
    context: Context,
    val msg: String = "",
    val width: Float = 200f,
) : EditText(context,null, R.attr.editTextFilledStyle) {
    // MD3 palette
    var md3PrimaryColor: Int = MaterialColor.TEAL_500.colorValue
    var md3Error: Boolean = false
        set(value) { field = value; invalidate() }

    private var isFocusedState = false
    private var isHoveredState = false

    private val md3Background = object : Drawable() {
        override fun draw(canvas: Canvas) {
            val r = context.dp(8f).toFloat()
            val strokeWFocused = context.dp(2f).toFloat()
            val strokeWDefault = context.dp(1f).toFloat()

            val container = Paint.obtain()
            val stroke = Paint.obtain()

            // Container fill (filled box)
            val containerColor = if (isEnabled) MaterialColor.GRAY_100.colorValue else MaterialColor.GRAY_200.colorValue
            container.color = containerColor
            container.style = Paint.Style.FILL.ordinal
            canvas.drawRoundRect(bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat(), r, container)

            // Outline
            val strokeColor = when {
                md3Error -> MaterialColor.RED_500.colorValue
                isFocusedState -> md3PrimaryColor
                isHoveredState -> MaterialColor.GRAY_600.colorValue
                else -> MaterialColor.GRAY_400.colorValue
            }
            val strokeW = if (md3Error || isFocusedState) strokeWFocused else strokeWDefault
            stroke.color = strokeColor
            stroke.style = Paint.Style.STROKE.ordinal
            stroke.strokeWidth = strokeW
            val half = strokeW / 2f
            canvas.drawRoundRect(bounds.left + half, bounds.top + half, bounds.right - half, bounds.bottom - half, r, stroke)

            container.recycle()
            stroke.recycle()
        }
    }

    init {
        hint = msg
        // Content padding per MD3 (16dp horizontal, 12dp vertical)
        setPadding(context.dp(16f), context.dp(12f), context.dp(16f), context.dp(12f))
        // Colors
      //  setTextColor(MaterialColor.GRAY_900.colorValue)
       // setHintTextColor(MaterialColor.GRAY_600.colorValue)
        // Background
       // background = md3Background
        // Focus/hover listeners to update states
        setOnFocusChangeListener { _: View, hasFocus: Boolean ->
            isFocusedState = hasFocus
            invalidate()
        }
        setOnHoverListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_HOVER_ENTER -> { isHoveredState = true; invalidate(); true }
                MotionEvent.ACTION_HOVER_EXIT -> { isHoveredState = false; invalidate(); true }
                else -> false
            }
        }
        layoutParams = LinearLayout.LayoutParams(
            dp(width),
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(16f)
            gravity = Gravity.CENTER_HORIZONTAL
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        invalidate()
    }

    fun setErrorEnabled(enabled: Boolean) {
        md3Error = enabled
    }
}