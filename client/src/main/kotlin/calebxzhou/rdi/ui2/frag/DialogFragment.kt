package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.util.*
import icyllis.modernui.graphics.Canvas
import icyllis.modernui.graphics.Paint
import icyllis.modernui.graphics.drawable.Drawable
import icyllis.modernui.view.Gravity
import org.lwjgl.util.tinyfd.TinyFileDialogs

fun alertErr(msg: String) {
    uiThread {
        mc go DialogFragment(msg, lvl = RMessageLevel.ERR)
    }
}
fun alertErrOs(msg: String){
    TinyFileDialogs.tinyfd_messageBox("RDI错误", msg, "ok", "error", true)
}
fun alertOk(msg: String) {
    uiThread {
        mc go DialogFragment(msg, lvl = RMessageLevel.OK)
    }
}
fun confirm(msg: String, onNo: () -> Unit = {}, onYes: () -> Unit={}) {
    uiThread {
        mc go DialogFragment(msg, RDialogType.CONFIRM, onYes = onYes, onNo = onNo)
    }
}
enum class RDialogType {
    CONFIRM, ALERT
}
class DialogFragment(
    val msg: String,
    val type: RDialogType = RDialogType.ALERT,
    val lvl: RMessageLevel = RMessageLevel.INFO,
    val yesText: String = if (type == RDialogType.ALERT) "明白" else "是",
    val noText: String = "否",
    val onYes: () -> Unit = { },
    val onNo: () -> Unit = { }
) : RFragment() {
    val prevScreen = mc.screen
    override var showTitle = false
    override var showCloseButton = false
    override fun initContent() {
        contentLayout.apply {
            frameLayout() {
                paddingDp(16)
                // This is the important change - setting the frame to be centered in its parent
                layoutParams = linearLayoutParam(PARENT, PARENT)

                // The dialog content container
                frameLayout() {
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
                    textView() {
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
                    textView() {
                        text = msg
                        layoutParams = frameLayoutParam {
                            gravity = Gravity.CENTER
                            topMargin = dp(16f)
                            bottomMargin = dp(16f)
                        }
                        gravity = Gravity.CENTER
                    }
                    // OK button
                    iconButton("success", yesText,init= {
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
                    })

                    // Cancel button (only for non-alert dialogs)
                    if (type != RDialogType.ALERT) {
                        iconButton("error",noText,init= {
                            layoutParams = frameLayoutParam(SELF, SELF) {
                                gravity = Gravity.BOTTOM or Gravity.START
                                marginStart = dp(16f)
                                bottomMargin = dp(16f)
                            }
                            setOnClickListener {
                                close()
                                onNo() }
                        })
                    }

                    clipChildren = true
                }
            }
        }

    }

}