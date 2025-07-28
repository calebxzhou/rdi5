package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.isMcStarted
import calebxzhou.rdi.util.mc
import icyllis.modernui.animation.ObjectAnimator
import icyllis.modernui.mc.MuiScreen
import icyllis.modernui.view.Gravity
import icyllis.modernui.view.View

class LoadingFragment : RFragment("加载中"){
    companion object{
        fun show() {
            if (isMcStarted) {
                mc.screen?.let { screen ->
                    mc.go(LoadingFragment())
                }
            }

        }
        fun close() {
            if (isMcStarted) {
                mc.screen?.let { screen ->
                    if (screen is MuiScreen && screen.fragment is LoadingFragment)
                        screen.onClose()
                }
            }
        }
    }
    val loadIcon = iconDrawable("loading")
    override fun initContent() {
        contentLayout.apply {
            frameLayout() {
                layoutParams = linearLayoutParam(PARENT, PARENT)
                paddingDp(16)
                this += frameLayout() {
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
    }

}