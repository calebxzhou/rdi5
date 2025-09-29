package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.localserver.LOCAL_PORT
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.model.Room
import calebxzhou.rdi.net.RServer
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
                layoutParams = frameLayoutParam(dp(480f), dp(320f)) {
                    gravity = Gravity.CENTER
                }

                // Add "rdi" text as child of strip, vertically centered
                textView() {
                    text = """
                        载入完成！
                        使用浏览器打开https://nng3.calebxzhou.cn:35800
                        输入连接码 $LOCAL_PORT
                    """.trimIndent()
                    setTextColor(0xFFFFFFFF.toInt())
                }

                // Add "5" text directly below the "i"

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