package calebxzhou.rdi.ui2.component

import calebxzhou.rdi.ui2.iconDrawable
import icyllis.modernui.core.Context
import icyllis.modernui.widget.ImageView

class HttpImageView(context: Context) : ImageView(context) {
    private val loadingImg = iconDrawable("loading")
    private val failLoadImg = iconDrawable("question")
}