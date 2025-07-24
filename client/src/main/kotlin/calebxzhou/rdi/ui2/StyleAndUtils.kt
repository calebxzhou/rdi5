package calebxzhou.rdi.ui2

import icyllis.modernui.fragment.Fragment
import icyllis.modernui.graphics.Canvas
import icyllis.modernui.graphics.Paint
import icyllis.modernui.graphics.drawable.Drawable
import icyllis.modernui.graphics.drawable.ImageDrawable
import icyllis.modernui.view.View
import icyllis.modernui.view.ViewGroup

/**
 * calebxzhou @ 2025-07-22 19:43
 */
val BG_GRAY_BORDER
    get() = object : Drawable() {
    override fun draw(canvas: Canvas) {
        val paint = Paint.obtain()
        paint.setRGBA(128, 128, 128, 255)
        paint.style = Paint.Style.STROKE.ordinal
        paint.strokeWidth = 2f
        canvas.drawRect(bounds, paint)
        paint.recycle()
    }
}
fun View.padding8dp() {
    setPadding(dp(8f), dp(8f), dp(8f), dp(8f))
}

fun View.paddingDp(left: Int, top: Int, right: Int, bottom: Int) {
    setPadding(dp(left.toFloat()), dp(top.toFloat()), dp(right.toFloat()), dp(bottom.toFloat()))
}

fun View.paddingDp(all: Int) {
    paddingDp(all, all, all, all)
}
//frag的上下文
val Fragment.fctx
    get() = requireContext()

operator fun ViewGroup.plusAssign(view: View) {
    addView(view)
}
fun rdiDrawable(path: String) = ImageDrawable("rdi","${path}.png")
fun iconDrawable(filename: String) = ImageDrawable("rdi","gui/icons/${filename}.png")