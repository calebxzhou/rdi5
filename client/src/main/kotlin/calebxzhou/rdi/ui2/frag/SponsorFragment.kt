package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.ui2.frameLayoutParam
import calebxzhou.rdi.ui2.imageView
import calebxzhou.rdi.ui2.rdiDrawable
import icyllis.modernui.view.Gravity
import calebxzhou.rdi.ui2.SELF

class SponsorFragment : RFragment("赞助") {
    val img = rdiDrawable("sponsor.png")
    init {
        contentViewInit = {
            gravity = Gravity.CENTER
            imageView {
                layoutParams = frameLayoutParam(SELF, SELF).apply {
                    gravity = Gravity.CENTER
                }
                setImageDrawable(img)
            }
        }
    }
}