package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.ui.RMessageLevel
import calebxzhou.rdi.ui.general.RDialogType
import calebxzhou.rdi.util.goFrag
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.ui2.*
import icyllis.modernui.graphics.Paint
import icyllis.modernui.graphics.drawable.Drawable
import icyllis.modernui.mc.UIManager
import icyllis.modernui.util.DataSet
import icyllis.modernui.view.Gravity
import icyllis.modernui.view.LayoutInflater
import icyllis.modernui.view.View
import icyllis.modernui.view.ViewGroup
import icyllis.modernui.graphics.Canvas
import calebxzhou.rdi.ui2.plusAssign
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.mcScreen
import calebxzhou.rdi.util.renderThread
import calebxzhou.rdi.util.set

fun alertErr(msg: String) {
    renderThread {
        mc set DialogFragment(msg, lvl = RMessageLevel.ERR).mcScreen
    }
}

fun alertOk(msg: String) {
    renderThread {
        mc set DialogFragment(msg, lvl = RMessageLevel.OK).mcScreen
    }
}

class DialogFragment(
    val msg: String,
    val type: RDialogType = RDialogType.ALERT,
    val lvl: RMessageLevel = RMessageLevel.INFO,
    val yesText: String = if (type == RDialogType.ALERT) "明白" else "是",
    val noText: String = "否",
    val onYes: () -> Unit = { },
    val onNo: () -> Unit = {}
) : RFragment() {
    val prevScreen = mc.screen
    override var showTitle = false
    override var showCloseButton = false
    override fun initContent() {
        contentLayout +=  frameLayout(fctx) {
            paddingDp(16)
            // This is the important change - setting the frame to be centered in its parent
            layoutParams = linearLayoutParam(PARENT, PARENT)

            // The dialog content container
            this += frameLayout(fctx) {
                paddingDp(16)
                layoutParams = frameLayoutParam(dp(480f), dp(320f)) {
                    gravity = Gravity.CENTER
                }
                background = object : Drawable() {
                    override fun draw(canvas: Canvas) {
                        val paint = Paint.obtain()
                        val b = getBounds()
                        when (lvl) {
                            RMessageLevel.INFO -> {
                                paint.setRGBA(0, 0, 255, 80)
                            }

                            RMessageLevel.WARN -> {
                                paint.setRGBA(255, 255, 0, 80)
                            }

                            RMessageLevel.ERR -> {
                                paint.setRGBA(255, 0, 0, 80)
                            }

                            RMessageLevel.OK -> {
                                paint.setRGBA(0, 255, 0, 80)
                            }
                        }
                        paint.style = Paint.Style.STROKE.ordinal
                        paint.strokeWidth = 16f
                        canvas.drawRoundRect(
                            b.left.toFloat(),
                            b.top.toFloat(),
                            b.right.toFloat(),
                            b.bottom.toFloat(),
                            64f,
                            paint
                        )
                        paint.recycle()
                    }
                }

                // Title text
                this += textView(fctx) {
                    text = when (lvl) {
                        RMessageLevel.INFO -> "提示"
                        RMessageLevel.WARN -> "警告"
                        RMessageLevel.ERR -> "错误"
                        RMessageLevel.OK -> "成功"
                    }
                    layoutParams = frameLayoutParam {
                        gravity = Gravity.CENTER_HORIZONTAL
                    }
                    gravity = Gravity.CENTER_HORIZONTAL
                }

                // Message content
                this += textView(fctx) {
                    text = msg
                    layoutParams = frameLayoutParam {
                        gravity = Gravity.CENTER
                        topMargin = dp(16f)
                        bottomMargin = dp(16f)
                    }
                    gravity = Gravity.CENTER
                }

                // OK button
                this += button(fctx) {
                    text = yesText
                    layoutParams = frameLayoutParam(SELF, SELF) {
                        gravity = if (type == RDialogType.ALERT) {
                            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                        } else {
                            Gravity.BOTTOM or Gravity.END
                        }
                        bottomMargin = dp(16f)
                    }
                    setOnClickListener {
                        onYes()
                        renderThread {

                            mc set prevScreen
                        }
                    }
                }

                // Cancel button (only for non-alert dialogs)
                if (type != RDialogType.ALERT) {
                    this += button(fctx) {
                        text = noText
                        layoutParams = frameLayoutParam(SELF, SELF) {
                            gravity = Gravity.BOTTOM or Gravity.START
                            marginStart = dp(16f)
                            bottomMargin = dp(16f)
                        }
                        setOnClickListener { onNo() }
                    }
                }

                clipChildren = true
            }
        }

    }

}