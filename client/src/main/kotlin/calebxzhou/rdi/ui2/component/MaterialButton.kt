package calebxzhou.rdi.ui2.component

import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.paddingDp
import icyllis.modernui.core.Context
import icyllis.modernui.graphics.Canvas
import icyllis.modernui.graphics.Paint
import icyllis.modernui.graphics.drawable.Drawable

/**
 * A material-styled button with convenient APIs for predefined styles
 * used by bottom options in fragments.
 */
class MaterialButton(context: Context, val color: MaterialColor = MaterialColor.WHITE, onClick: (RButton) -> Unit = {}) : RButton(context, onClick) {

	init {
		// Default comfortable touch target
		paddingDp(16, 8, 16, 8)
        background = object : Drawable() {
            override fun draw(canvas: Canvas) {
                val paint = Paint.obtain()
                paint.color = color.colorValue
                // Keep consistent with existing usage of Paint style
                paint.style = Paint.Style.FILL.ordinal
                // Rounded rect background
                canvas.drawRoundRect(
                    bounds.left.toFloat(),
                    bounds.top.toFloat(),
                    bounds.right.toFloat(),
                    bounds.bottom.toFloat(),
                    48f,
                    paint
                )
                paint.recycle()
            }
        }

        // Ensure text has sufficient contrast
        val textColor = if (isDarkColor(color)) MaterialColor.WHITE.colorValue else MaterialColor.BLACK.colorValue
        setTextColor(textColor)
	}

	/** Apply a filled style with the given material color. */
	fun setMaterialColor(color: MaterialColor) {

	}

	private fun isDarkColor(color: MaterialColor): Boolean {
		val colorValue = color.colorValue
		val red = (colorValue ushr 16) and 0xFF
		val green = (colorValue ushr 8) and 0xFF
		val blue = colorValue and 0xFF
		val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255
		return luminance < 0.5
	}
}