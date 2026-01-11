package calebxzhou.rdi.client.ui.frag

import calebxzhou.rdi.client.ui.SELF
import calebxzhou.rdi.client.ui.frameLayoutParam
import calebxzhou.rdi.client.ui.imageView
import calebxzhou.rdi.client.ui.rdiDrawable
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