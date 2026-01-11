package calebxzhou.rdi.client.ui.component

import calebxzhou.rdi.common.model.Host
import calebxzhou.rdi.client.ui.*
import icyllis.modernui.core.Context
import icyllis.modernui.graphics.Paint
import icyllis.modernui.graphics.drawable.ImageDrawable
import icyllis.modernui.text.TextUtils
import icyllis.modernui.view.Gravity
import icyllis.modernui.widget.ImageView
import icyllis.modernui.widget.LinearLayout
import java.io.ByteArrayInputStream

class HostCard(
    ctx: Context,
    val data: Host.Vo,
    private val onClickPlay: (Host.Vo) -> Unit = {},
) : LinearLayout(ctx) {
    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        imageView {
            scaleType = ImageView.ScaleType.CENTER_CROP
            layoutParams = linearLayoutParam(dp(50f), dp(50f))
            setImageDrawable(data.icon?.let { ImageDrawable(ByteArrayInputStream(it)) } ?: iconDrawable("team"))
        }
        linearLayout {
            orientation = VERTICAL
            gravity = Gravity.START
            layoutParams = linearLayoutParam(0, SELF) {
                weight = 1f
                leftMargin = dp(12f)
            }
            textView(data.name) {
                ellipsize = TextUtils.TruncateAt.END
                maxLines = 1
            }
            textView("${data.modpackName} ${data.packVer}") {
                setTextColor(MaterialColor.GRAY_500.colorValue)
                textSize=12f
                ellipsize = TextUtils.TruncateAt.END
                maxLines = 1
            }
            linearLayout {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = linearLayoutParam(PARENT, SELF) {
                    topMargin = dp(6f)
                }

                textView(data.intro) {
                    layoutParams = linearLayoutParam(0, SELF) {
                        weight = 1f
                        rightMargin = dp(12f)
                    }
                    setTextColor(MaterialColor.GRAY_500.colorValue)
                    textSize = 10f
                    ellipsize = TextUtils.TruncateAt.END
                    maxLines = 2
                }

                textView("\u25B6") {
                    val buttonSize = dp(28f)
                    layoutParams = linearLayoutParam(buttonSize, buttonSize)
                    gravity = Gravity.CENTER
                    textSize = 16f
                    setTextColor(MaterialColor.WHITE.colorValue)
                    background = drawable { canvas ->
                        val paint = Paint.obtain()
                        paint.color = MaterialColor.GREEN_900.colorValue
                        paint.style = Paint.Style.FILL.ordinal
                        val diameter = (bounds.right - bounds.left)
                            .coerceAtMost(bounds.bottom - bounds.top)
                            .toFloat()
                        val radius = diameter / 2f
                        val cx = (bounds.left + bounds.right) / 2f
                        val cy = (bounds.top + bounds.bottom) / 2f
                        canvas.drawCircle(cx.toFloat(), cy.toFloat(), radius, paint)
                        paint.recycle()
                    }
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { onClickPlay(data) }
                }
            }

        }

    }
}