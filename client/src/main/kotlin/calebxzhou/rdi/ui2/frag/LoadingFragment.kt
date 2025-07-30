package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.ui2.fctx
import calebxzhou.rdi.ui2.frameLayoutParam
import calebxzhou.rdi.ui2.iconDrawable
import calebxzhou.rdi.util.isMcStarted
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.renderThread
import icyllis.modernui.animation.ObjectAnimator
import icyllis.modernui.mc.MuiScreen
import icyllis.modernui.util.DataSet
import icyllis.modernui.view.Gravity
import icyllis.modernui.view.LayoutInflater
import icyllis.modernui.view.View
import icyllis.modernui.view.ViewGroup
import icyllis.modernui.widget.FrameLayout

class LoadingFragment : RFragment("加载中"){
    companion object{

        fun close() {
            if (isMcStarted) {
                renderThread {

                    mc.screen?.let { screen ->
                        if (screen is MuiScreen && screen.fragment is LoadingFragment)
                            screen.onClose()
                    }
                }
            }
        }
    }
    val loadIcon = iconDrawable("loading")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: DataSet?): View {
        return FrameLayout(fctx).apply {
            layoutParams = frameLayoutParam(dp(200f), dp(200f)) {
                gravity = Gravity.CENTER
            }
            background = loadIcon
            // Add infinite rotation animation
            ObjectAnimator.ofFloat(this, View.ROTATION, -360f, 360f).apply {
                duration = 1000  // One rotation per second
                repeatCount = ObjectAnimator.INFINITE
                start()
            }
        }
    }

}