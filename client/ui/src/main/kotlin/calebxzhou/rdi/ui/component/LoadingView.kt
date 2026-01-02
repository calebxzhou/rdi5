package calebxzhou.rdi.ui.component

import calebxzhou.rdi.ui.*
import calebxzhou.rdi.ui.component.LoadingView.Companion.ID
import calebxzhou.rdi.ui.frag.RFragment
import icyllis.modernui.animation.ObjectAnimator
import icyllis.modernui.core.Context
import icyllis.modernui.graphics.drawable.ColorDrawable
import icyllis.modernui.view.Gravity
import icyllis.modernui.view.View
import icyllis.modernui.view.ViewGroup
import icyllis.modernui.widget.FrameLayout

fun RFragment.showLoading() = uiThread{
    val root = (view as? ViewGroup) ?: return@uiThread
    if (root.findViewById<View>(ID) != null) return@uiThread
    root.addView(LoadingView(fctx))
}
fun RFragment.closeLoading() = uiThread{
    (view as? ViewGroup)?.findViewById<View>(ID)?.let { (view as ViewGroup).removeView(it) }
}
class LoadingView(context: Context): FrameLayout(context){
    companion object{
        const val  ID = 0x666666


    }



    private val loadIcon = iconDrawable("loading")
    init {
        // Fullscreen overlay
        layoutParams = frameLayoutParam(PARENT, PARENT)
        background = ColorDrawable(0x80000000.toInt()) // 50% dim
        isClickable = true // block touches

        // Centered spinner
        val spinner = View(context).apply { background = loadIcon }
        addView(spinner, frameLayoutParam(dp(240f), dp(240f)) { gravity = Gravity.CENTER })

        // Infinite rotation animation
        ObjectAnimator.ofFloat(spinner, View.ROTATION, 0f, 360f).apply {
            duration = 1000
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
        id = ID
    }

}