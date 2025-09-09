package calebxzhou.rdi.ui2.component

import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.dp
import calebxzhou.rdi.ui2.iconDrawable
import calebxzhou.rdi.ui2.paddingDp
import icyllis.modernui.R
import icyllis.modernui.core.Context
import icyllis.modernui.graphics.Canvas
import icyllis.modernui.graphics.Paint
import icyllis.modernui.graphics.drawable.Drawable
import icyllis.modernui.view.Gravity
import icyllis.modernui.view.MotionEvent
import icyllis.modernui.view.View
import icyllis.modernui.view.ViewGroup
import icyllis.modernui.widget.*
// (animation removed)

/**
 * Material Design 3 Text Field with:
 * - Floating label
 * - Optional leading icon
 * - Optional trailing clear button
 * - Bottom indicator line (focused/error)
 */
class RTextField(
    context: Context,
    var label: String = "",
    val widthDp: Float = 120f,
    var icon: String? = null,
    var clearable: Boolean = true,
) : FrameLayout(context) {

    var md3PrimaryColor: Int = MaterialColor.TEAL_500.colorValue
        set(value) { field = value; invalidate() }
    var md3Error: Boolean = false
        set(value) { field = value; invalidate() }

    private var isFocusedState = false
    private var isHoveredState = false
    private var constructed = false
    private var lastFloating = false
    // animations removed; no timers

    // Views
    private val labelTv = TextView(context)
    private val row = LinearLayout(context)
    private val rowLp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
        gravity = Gravity.TOP or Gravity.START
        topMargin = context.dp(8f)
    }
    private val iconStart = ImageView(context)
    private val clearBtn = TextView(context)
    val edit: EditText = object : EditText(context, null,  R.attr.editTextFilledStyle) {
        override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
            super.onTextChanged(text, start, before, count)
            updateLabelAndClear()
        }
        init {
            setTextAppearance(R.attr.textAppearanceLabelLarge)
        }
    }

    private val backgroundDrawable = object : Drawable() {
        override fun draw(canvas: Canvas) {
            val r = context.dp(12f).toFloat()
            val container = Paint.obtain()
            val stroke = Paint.obtain()

            // Container fill
            val fillColor = if (isEnabled) MaterialColor.GRAY_100.colorValue else MaterialColor.GRAY_200.colorValue
            container.color = fillColor
            container.style = Paint.Style.FILL.ordinal
            canvas.drawRoundRect(bounds.left.toFloat(), bounds.top.toFloat(), bounds.right.toFloat(), bounds.bottom.toFloat(), r, container)

            // Bottom indicator line
            val lineColor = when {
                md3Error -> MaterialColor.RED_500.colorValue
                isFocusedState -> md3PrimaryColor
                isHoveredState -> MaterialColor.GRAY_600.colorValue
                else -> MaterialColor.GRAY_400.colorValue
            }
            val lineHeight = if (isFocusedState || md3Error) context.dp(2f).toFloat() else context.dp(1f).toFloat()
            stroke.color = lineColor
            stroke.style = Paint.Style.FILL.ordinal
            canvas.drawRect(
                bounds.left.toFloat() + context.dp(12f),
                bounds.bottom - lineHeight,
                bounds.right.toFloat() - context.dp(12f),
                bounds.bottom.toFloat(),
                stroke
            )

            container.recycle(); stroke.recycle()
        }
    }

    init {
    // Container look
    background = backgroundDrawable
    setPadding(context.dp(12f), context.dp(12f), context.dp(12f), context.dp(12f))
    // Use widthDp as a minimum width for the whole field; content row expands to fill available space
    minimumWidth = context.dp(widthDp)

        // Label (floating)
        labelTv.text = label
    labelTv.textSize = 12f
        labelTv.setTextColor(MaterialColor.INDIGO_500.colorValue)
    labelTv.translationY = context.dp(2f).toFloat()
        addView(labelTv, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            leftMargin = context.dp(12f)
            topMargin = context.dp(6f)
            gravity = Gravity.TOP or Gravity.START
        })

        // Row: icon + input + clear
    row.orientation = LinearLayout.HORIZONTAL
    row.gravity = Gravity.CENTER_VERTICAL
    addView(row, rowLp)

        // Leading icon
        if (icon != null) {
            iconStart.setImageDrawable(iconDrawable(icon!!))
            val sz = context.dp(20f)
            val lp = LinearLayout.LayoutParams(sz, sz).apply { rightMargin = context.dp(8f) }
            row.addView(iconStart, lp)
        }

        // EditText styling (transparent inside; caret/hint colors)
        edit.setBackground(null)
        edit.hint = label
        edit.setTextColor(MaterialColor.GRAY_900.colorValue)
        edit.setHintTextColor(MaterialColor.GRAY_600.colorValue)
    edit.paddingDp(0, 8, 0, 8)
    edit.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(edit)

        // Trailing clear button
        val clearSize = context.dp(18f)
        clearBtn.text = "❌"
        clearBtn.setOnClickListener { edit.setText("") }
        clearBtn.visibility = View.GONE
        row.addView(clearBtn, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            leftMargin = context.dp(8f)
        })

        // Focus/hover
        edit.setOnFocusChangeListener { _, hasFocus ->
            isFocusedState = hasFocus
            updateLabelAndClear()
            invalidate()
        }
        setOnHoverListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_HOVER_ENTER -> { isHoveredState = true; invalidate(); true }
                MotionEvent.ACTION_HOVER_EXIT -> { isHoveredState = false; invalidate(); true }
                else -> false
            }
        }

    // Finish construction and update
    constructed = true
    updateLabelAndClear()
    }

    private fun updateLabelAndClear() {
    if (!constructed) return
        val hasText = edit.text?.isNotEmpty() == true
        // Label floats on focus or when text present
        val shouldFloat = isFocusedState || hasText
        if (shouldFloat) {
            labelTv.visibility = View.VISIBLE
            edit.hint = null
            labelTv.setTextColor(if (md3Error) MaterialColor.RED_500.colorValue else md3PrimaryColor)
            labelTv.translationX = 0f
            labelTv.translationY = 0f
            labelTv.scaleX = 1f
            labelTv.scaleY = 1f
            val labelHeight = if (labelTv.measuredHeight > 0) labelTv.measuredHeight else context.dp(16f)
            rowLp.topMargin = context.dp(6f) + labelHeight + context.dp(4f)
        } else {
            labelTv.visibility = View.GONE
            edit.hint = label
            labelTv.setTextColor(MaterialColor.INDIGO_500.colorValue)
            rowLp.topMargin = context.dp(8f)
        }
        row.layoutParams = rowLp
        row.requestLayout()
        // Clear button
        clearBtn.visibility = if (clearable && hasText) View.VISIBLE else View.GONE
        lastFloating = shouldFloat
    }
    // animations removed; no compute/anim helpers

    fun setText(value: String) { edit.setText(value); updateLabelAndClear() }
    fun getText(): String = edit.text?.toString() ?: ""

    fun setLeadingIcon(name: String?) {
        icon = name
        if (name == null) {
            if (iconStart.parent === row) row.removeView(iconStart)
        } else {
            iconStart.setImageDrawable(iconDrawable(name))
            if (iconStart.parent == null) row.addView(iconStart, 0, LinearLayout.LayoutParams(context.dp(20f), context.dp(20f)).apply { rightMargin = context.dp(8f) })
        }
    }

    fun setErrorEnabled(enabled: Boolean) { md3Error = enabled; updateLabelAndClear(); invalidate() }
}
