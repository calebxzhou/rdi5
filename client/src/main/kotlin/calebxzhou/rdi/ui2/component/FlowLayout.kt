package calebxzhou.rdi.ui2.component

import icyllis.modernui.core.Context
import icyllis.modernui.util.AttributeSet
import icyllis.modernui.view.MeasureSpec
import icyllis.modernui.view.View
import icyllis.modernui.view.ViewGroup
import kotlin.math.max
import kotlin.math.min

class FlowLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs) {

    private val lines = mutableListOf<List<View>>()
    private val lineHeights = mutableListOf<Int>()

    override fun generateDefaultLayoutParams(): LayoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    override fun generateLayoutParams(p: ViewGroup.LayoutParams): ViewGroup.LayoutParams = LayoutParams(p)

    override fun checkLayoutParams(p: ViewGroup.LayoutParams): Boolean = p is MarginLayoutParams

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val maxLineWidth = when (widthMode) {
            MeasureSpec.UNSPECIFIED -> Int.MAX_VALUE
            else -> max(0, widthSize - paddingLeft - paddingRight)
        }

        var currentLineWidth = 0
        var currentLineHeight = 0
        var requiredWidth = 0
        var requiredHeight = paddingTop + paddingBottom

        val currentLineViews = mutableListOf<View>()
        lines.clear()
        lineHeights.clear()

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) continue

            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
            val lp = child.layoutParams as MarginLayoutParams
            val childWidth = child.measuredWidth + lp.leftMargin + lp.rightMargin
            val childHeight = child.measuredHeight + lp.topMargin + lp.bottomMargin

            if (currentLineViews.isNotEmpty() && currentLineWidth + childWidth > maxLineWidth) {
                lines += currentLineViews.toList()
                lineHeights += currentLineHeight
                requiredWidth = max(requiredWidth, currentLineWidth)
                requiredHeight += currentLineHeight

                currentLineViews.clear()
                currentLineWidth = 0
                currentLineHeight = 0
            }

            currentLineViews += child
            currentLineWidth += childWidth
            currentLineHeight = max(currentLineHeight, childHeight)
        }

        if (currentLineViews.isNotEmpty()) {
            lines += currentLineViews.toList()
            lineHeights += currentLineHeight
            requiredWidth = max(requiredWidth, currentLineWidth)
            requiredHeight += currentLineHeight
        }

        if (lines.isEmpty()) {
            requiredWidth = 0
            requiredHeight = paddingTop + paddingBottom
        }

        val finalWidth = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> min(requiredWidth + paddingLeft + paddingRight, widthSize)
            else -> requiredWidth + paddingLeft + paddingRight
        }

        val finalHeight = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> min(requiredHeight, heightSize)
            else -> requiredHeight
        }

        setMeasuredDimension(finalWidth, finalHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var curLeft = paddingLeft
        var curTop = paddingTop

        for (lineIndex in lines.indices) {
            val lineViews = lines[lineIndex]
            val lineHeight = lineHeights.getOrElse(lineIndex) { 0 }
            curLeft = paddingLeft

            for (view in lineViews) {
                if (view.visibility == View.GONE) continue
                val lp = view.layoutParams as MarginLayoutParams
                val left = curLeft + lp.leftMargin
                val top = curTop + lp.topMargin
                val right = left + view.measuredWidth
                val bottom = top + view.measuredHeight
                view.layout(left, top, right, bottom)
                curLeft = right + lp.rightMargin
            }

            curTop += lineHeight
        }
    }

    class LayoutParams : MarginLayoutParams {
        constructor(width: Int, height: Int) : super(width, height)
        constructor(source: ViewGroup.LayoutParams?) : super(source)
    }
}
