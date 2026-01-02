package calebxzhou.rdi.ui.frag

import calebxzhou.rdi.ui.SELF
import calebxzhou.rdi.ui.frameLayoutParam
import calebxzhou.rdi.ui.imageView
import calebxzhou.rdi.ui.rdiDrawable
import icyllis.modernui.view.Gravity

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