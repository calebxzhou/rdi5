package calebxzhou.rdi.ui2.component

import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.paddingDp
import icyllis.modernui.R
import icyllis.modernui.core.Context
import icyllis.modernui.graphics.Canvas
import icyllis.modernui.graphics.Paint
import icyllis.modernui.graphics.drawable.Drawable
import icyllis.modernui.widget.Button
 

open class RButton(
    context: Context,
    val color: MaterialColor = MaterialColor.WHITE,
    val onClick: (RButton) -> Unit = {},
): Button(context,null, R.attr.buttonStyle,R.style.Widget_Material3_Button_IconButton) {




    init {
        // Ensure normal click works when hold-to-confirm is not enabled
        setOnClickListener { onClick(this) }
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
        val textColor = if (color.isDarkColor) MaterialColor.WHITE.colorValue else MaterialColor.BLACK.colorValue
        setTextColor(textColor)
    }


}