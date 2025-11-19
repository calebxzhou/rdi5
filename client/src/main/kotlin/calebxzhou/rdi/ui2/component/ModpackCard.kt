package calebxzhou.rdi.ui2.component

import calebxzhou.rdi.model.pack.ModpackVo
import calebxzhou.rdi.net.humanSize
import calebxzhou.rdi.ui2.*
import icyllis.modernui.core.Context
import icyllis.modernui.graphics.Paint
import icyllis.modernui.graphics.drawable.ImageDrawable
import icyllis.modernui.text.TextUtils
import icyllis.modernui.text.Typeface
import icyllis.modernui.view.Gravity
import icyllis.modernui.widget.ImageView
import icyllis.modernui.widget.LinearLayout
import icyllis.modernui.widget.TextView
import java.io.ByteArrayInputStream

class ModpackCard(
    context: Context,
    val modpack: ModpackVo,
) : LinearLayout(context) {

    private lateinit var iconView: ImageView
    private lateinit var nameView: TextView
    private lateinit var authorView: TextView
    private lateinit var infoView: TextView
    private lateinit var descriptionView: TextView

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        padding8dp()
        layoutParams = linearLayoutParam(PARENT, SELF) {
            bottomMargin = context.dp(12f)
        }
        background = cardBackground()
        isClickable = true
        isFocusable = true

        iconView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = iconBackground()
            padding8dp()
        }
        addView(iconView, linearLayoutParam(context.dp(56f), context.dp(56f))  )

        linearLayout {
            gravity = Gravity.START
            vertical()
            nameView = textView {
                text = modpack.name
                textSize = 14f
                textStyle = Typeface.BOLD
                setTextColor(MaterialColor.GRAY_900.colorValue)
                ellipsize = TextUtils.TruncateAt.END
                maxLines = 1
            }

            authorView = textView {
                textSize = 13f
                textStyle = Typeface.ITALIC
                setTextColor(MaterialColor.GRAY_900.colorValue)
                ellipsize = TextUtils.TruncateAt.END
                maxLines = 1
            }
            infoView = textView {
                textSize = 13f
                textStyle = Typeface.ITALIC
                setTextColor(MaterialColor.GRAY_900.colorValue)
                ellipsize = TextUtils.TruncateAt.END
                maxLines = 1
            }
            descriptionView = textView {
                textSize = 13f
                setTextColor(MaterialColor.GRAY_700.colorValue)
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                setLineSpacing(0f, 1.2f)
            }
        }
        bindData()
    }

    private fun bindData() {
        nameView.text = modpack.name.ifBlank { "未命名模组包" }
        // Icon
        val iconBytes = modpack.icon
        val packIcon = if (iconBytes != null && iconBytes.isNotEmpty()) {
            try {
                ImageDrawable(ByteArrayInputStream(iconBytes))
            } catch (_: Exception) {
                null
            }
        } else null
        iconView.setImageDrawable(packIcon ?: iconDrawable("mcmod"))

        // Author and description directly from ModpackInfo
        authorView.text = "上传者：${modpack.authorName}"
        infoView.text="${modpack.fileSize.humanSize}+${modpack.modCount}mods "
        val description = modpack.info.trim().ifBlank { "暂无简介" }
        descriptionView.text = description

    }

    private fun addTag(icon: String?, label: String) {
        if (label.isBlank()) return

        val corner = context.dp(12f).toFloat()

        val tv = textView() {
            text = label
            textSize = 12f
            setTextColor(MaterialColor.GRAY_200.colorValue)
            setPadding(context.dp(8f), context.dp(4f), context.dp(8f), context.dp(4f))
            background = drawable { canvas ->
                val paint = Paint.obtain()
                paint.setRGBA(255, 255, 255, 25)
                paint.style = Paint.Style.FILL.ordinal
                canvas.drawRoundRect(
                    bounds.left.toFloat(),
                    bounds.top.toFloat(),
                    bounds.right.toFloat(),
                    bounds.bottom.toFloat(),
                    corner,
                    paint
                )
                paint.recycle()
            }
        }
        if (icon != null) tv.leadingIcon(icon)
    }

    private fun iconBackground() = drawable { canvas ->
        val paint = Paint.obtain()
        paint.setRGBA(240, 240, 240, 255)
        paint.style = Paint.Style.FILL.ordinal
        val radius = context.dp(12f).toFloat()
        canvas.drawRoundRect(
            bounds.left.toFloat(),
            bounds.top.toFloat(),
            bounds.right.toFloat(),
            bounds.bottom.toFloat(),
            radius,
            paint
        )
        paint.recycle()
    }

    private fun cardBackground() = drawable { canvas ->
        val paint = Paint.obtain()
        paint.setRGBA(255, 255, 255, 235)
        paint.style = Paint.Style.FILL.ordinal
        val radius = context.dp(16f).toFloat()
        canvas.drawRoundRect(
            bounds.left.toFloat(),
            bounds.top.toFloat(),
            bounds.right.toFloat(),
            bounds.bottom.toFloat(),
            radius,
            paint
        )
        paint.recycle()
    }
}