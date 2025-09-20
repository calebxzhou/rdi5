package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.Const
import calebxzhou.rdi.PACK
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.model.Room
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.RServer.Companion.OFFICIAL_DEBUG
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.set
import icyllis.modernui.animation.ObjectAnimator
import icyllis.modernui.animation.PropertyValuesHolder
import icyllis.modernui.animation.TimeInterpolator
import icyllis.modernui.graphics.drawable.ColorDrawable
import icyllis.modernui.mc.ui.CenterFragment2
import icyllis.modernui.util.DataSet
import icyllis.modernui.view.*
import icyllis.modernui.widget.FrameLayout
import icyllis.modernui.widget.LinearLayout

class TitleFragment : RFragment() {
    override var closable = false
    override var showTitle = false
    override fun initContent() {}

    init {
        RAccount.now=null
        Room.now= Room.DEFAULT
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: DataSet?): View =
        FrameLayout(fctx).apply {
            background = BG_IMAGE_MUI

            // Add semi-transparent gray strip with text children
            linearLayout() {
                orientation = LinearLayout.HORIZONTAL
                background = ColorDrawable(0xaa000000.toInt())
                layoutParams = linearLayoutParam() {
                    width = PARENT
                    height = dp(100f)
                    gravity = Gravity.BOTTOM

                    topMargin = (container?.measuredHeight ?: 0) * 2 / 3
                }

                // Add "rdi" text as child of strip, vertically centered
                textView() {
                    text = "RDi"
                    setTextColor(0xFFFFFFFF.toInt())
                    textSize = 64f
                    layoutParams = linearLayoutParam {
                        width = SELF
                        height = SELF
                        gravity = Gravity.CENTER_VERTICAL or Gravity.START
                        leftMargin = dp(16f)
                    }
                }

                // Add "5" text directly below the "i"
                textView() {
                    text = "5"
                    setTextColor(0xFFFFFFFF.toInt())
                    textSize = 28f
                    layoutParams = linearLayoutParam {
                        width = SELF
                        height = SELF
                        gravity = Gravity.TOP or Gravity.START
                    }
                }
                textView {
                    text = "SkyPro"
                    typeface = Fonts.CODE.typeface
                }
                // Add spacer to push Enter tip to the end
                this += View(context).apply {
                    layoutParams = linearLayoutParam {
                        width = 0
                        height = SELF
                        weight = 1f
                    }
                }

                // Add "Press Enter" tip at the right end
                textView() {
                    text = "按 Enter ➡️"
                    setTextColor(0xFFFFFFFF.toInt())
                    typeface = Fonts.ART.typeface
                    textSize = 24f
                    isClickable = true
                    setOnClickListener {
                        startMulti()
                    }
                    layoutParams = linearLayoutParam {
                        width = SELF
                        height = SELF
                        gravity = Gravity.CENTER_VERTICAL
                        marginEnd = dp(16f)
                    }

                    // Add breathing animation
                    val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.2f)
                    val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.2f)
                    val alpha = PropertyValuesHolder.ofFloat(View.ALPHA, 1f, 0.5f)

                    ObjectAnimator.ofPropertyValuesHolder(this, scaleX, scaleY, alpha).apply {
                        duration = 1000
                        repeatCount = ObjectAnimator.INFINITE
                        repeatMode = ObjectAnimator.REVERSE
                        interpolator = TimeInterpolator.ACCELERATE_DECELERATE
                    }.start()
                }
            }
            // Set keyboard listener for Enter key
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
            keyAction {
                enter {
                    startMulti()
                }
                KeyEvent.KEY_KP_0{
                    mc set CenterFragment2().mcScreen
                }
            }
        }

    fun startMulti() {
        RServer.now.connect()
    }

}