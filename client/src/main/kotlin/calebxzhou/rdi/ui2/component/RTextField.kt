package calebxzhou.rdi.ui2.component

import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.dp
import calebxzhou.rdi.ui2.linearLayout
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
import icyllis.modernui.text.method.PasswordTransformationMethod

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
    var widthDp: Float = 120f,
    var clearable: Boolean = true,
) : FrameLayout(context) {

    var nightMode: Boolean = true
        set(value) {
            field = value
            applyColors()
            invalidate()
        }
    var isPassword: Boolean = false
        set(value) {
            field = value
            if (value) {
                if (!passwordVisible) edit.setTransformationMethod(PasswordTransformationMethod.getInstance())
            } else {
                edit.setTransformationMethod(null)
            }
            configureTrailing()
        }
    var dropdownItems: List<String> = emptyList()
        set(value) {
            field = value
            configureTrailing()
            updateLabelAndClear()
        }
    var onDropdownItemSelected: ((String) -> Unit)? = null
    var md3PrimaryColor: Int = MaterialColor.TEAL_500.colorValue
        set(value) {
            field = value; invalidate()
        }
    var md3Error: Boolean = false
        set(value) {
            field = value; invalidate()
        }
    var multiLine: Boolean = false
        set(value) {
            field = value
            edit.setSingleLine(!value)
        }
    var focusShowLabel = true
    private var isFocusedState = false
    private var isHoveredState = false
    private var constructed = false
    private var lastFloating = false
    // animations removed; no timers

    // Views
    private val labelTv = TextView(context)
    private val row : LinearLayout
    private val rowLp =
        FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.TOP or Gravity.START
            topMargin = context.dp(8f)
        }
    private val clearBtn = TextView(context)
    private var passwordVisible = false
    val edit: EditText = object : EditText(context, null, R.attr.editTextFilledStyle) {
        override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
            super.onTextChanged(text, start, before, count)
            updateLabelAndClear()
        }

        init {
            setTextAppearance(R.attr.textAppearanceLabelLarge)
            setSingleLine(true)
            includeFontPadding = false
            minHeight = 0
        }
    }

    private val backgroundDrawable = object : Drawable() {
        override fun draw(canvas: Canvas) {
            val r = context.dp(12f).toFloat()
            val container = Paint.obtain()
            val stroke = Paint.obtain()

            // Container fill
            val fillColor =
                if (nightMode) 0xFF1E1E1E.toInt() else if (isEnabled) MaterialColor.GRAY_100.colorValue else MaterialColor.GRAY_200.colorValue
            container.color = fillColor
            container.style = Paint.Style.FILL.ordinal
            canvas.drawRoundRect(
                bounds.left.toFloat(),
                bounds.top.toFloat(),
                bounds.right.toFloat(),
                bounds.bottom.toFloat(),
                r,
                container
            )

            // Bottom indicator line
            val lineColor = when {
                md3Error -> MaterialColor.RED_500.colorValue
                isFocusedState -> md3PrimaryColor
                isHoveredState -> if (nightMode) MaterialColor.GRAY_300.colorValue else MaterialColor.GRAY_600.colorValue
                else -> if (nightMode) MaterialColor.GRAY_500.colorValue else MaterialColor.GRAY_400.colorValue
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
        // Slightly tighter vertical padding to make the field thinner
        setPadding(context.dp(12f), context.dp(8f), context.dp(12f), context.dp(8f))
        // Use widthDp as a minimum width for the whole field; content row expands to fill available space
        minimumWidth = context.dp(widthDp)

        // Label (floating)
        labelTv.text = label
        labelTv.textSize = 12f
        labelTv.setTextColor(MaterialColor.INDIGO_500.colorValue)
        labelTv.translationY = context.dp(2f).toFloat()
        addView(
            labelTv,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = context.dp(12f)
              //  topMargin = context.dp(6f)
                gravity = Gravity.TOP or Gravity.START
            })
        row = linearLayout {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = rowLp
        }

        // EditText styling (transparent inside; caret/hint colors)
        edit.setBackground(null)
        edit.hint = label
        // Reduce internal vertical padding for a thinner look
        edit.paddingDp(0, 4, 0, 4)
        edit.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(edit)

        // Trailing action (clear or eye depending on mode)
        clearBtn.text = "❌"
        clearBtn.visibility = View.GONE
        // Keep the trailing control from increasing the row height
        clearBtn.includeFontPadding = false
        clearBtn.setPadding(0, 0, 0, 0)
        row.addView(
            clearBtn,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = context.dp(8f)
            })

        configureTrailing()

        // Focus/hover
        edit.setOnFocusChangeListener { _, hasFocus ->
            isFocusedState = hasFocus
            updateLabelAndClear()
            invalidate()
        }
        setOnHoverListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_HOVER_ENTER -> {
                    isHoveredState = true; invalidate(); true
                }

                MotionEvent.ACTION_HOVER_EXIT -> {
                    isHoveredState = false; invalidate(); true
                }

                else -> false
            }
        }

        // Apply initial color scheme
        applyColors()

        // Finish construction and update
        constructed = true
        updateLabelAndClear()
        layoutParams = LinearLayout.LayoutParams(
            dp(widthDp),
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
    }

    val txt get() = edit.text.toString()
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
        // Trailing button visibility policy
        clearBtn.visibility = when {
            isPassword -> View.VISIBLE
            dropdownItems.isNotEmpty() -> View.VISIBLE
            clearable && hasText -> View.VISIBLE
            else -> View.GONE
        }
        lastFloating = shouldFloat
    }
    // animations removed; no compute/anim helpers

    private fun configureTrailing() {
        when {
            isPassword -> {
                // Eye toggle
                clearBtn.text = "👁"
                clearBtn.setOnClickListener {
                    passwordVisible = !passwordVisible
                    edit.setTransformationMethod(if (passwordVisible) null else PasswordTransformationMethod.getInstance())
                }
            }

            dropdownItems.isNotEmpty() -> {
                clearBtn.text = "▾"
                clearBtn.setOnClickListener { showDropdownMenu() }
            }

            else -> {
                // Clear button
                clearBtn.text = "❌"
                clearBtn.setOnClickListener { edit.setText("") }
            }
        }
    }

    private fun showDropdownMenu() {
        if (dropdownItems.isEmpty()) return
        val popup = PopupMenu(context, this)
        dropdownItems.forEachIndexed { index, item ->
            popup.menu.add(0, index, index, item).setOnMenuItemClickListener {
                text = item
                onDropdownItemSelected?.invoke(item)
                true
            }
        }
        popup.show()
    }

    fun openDropdown() = showDropdownMenu()

    private fun applyColors() {
        if (nightMode) {
            // Texts on dark
            edit.setTextColor(MaterialColor.WHITE.colorValue)
            edit.setHintTextColor(MaterialColor.GRAY_400.colorValue)
            clearBtn.setTextColor(MaterialColor.WHITE.colorValue)
        } else {
            edit.setTextColor(MaterialColor.GRAY_900.colorValue)
            edit.setHintTextColor(MaterialColor.GRAY_600.colorValue)
            clearBtn.setTextColor(MaterialColor.GRAY_900.colorValue)
        }
    }

    var text
        get() = edit.text.toString()
        set(value) {
            edit.setText(value); updateLabelAndClear()
        }

    fun setErrorEnabled(enabled: Boolean) {
        md3Error = enabled; updateLabelAndClear(); invalidate()
    }
}
