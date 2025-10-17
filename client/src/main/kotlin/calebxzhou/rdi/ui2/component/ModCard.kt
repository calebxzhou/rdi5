package calebxzhou.rdi.ui2.component

import calebxzhou.rdi.model.ModBriefVo
import calebxzhou.rdi.net.httpRequest
import calebxzhou.rdi.net.httpRequest_
import calebxzhou.rdi.net.success
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.util.ioScope
import icyllis.modernui.core.Context
import icyllis.modernui.graphics.Paint
import icyllis.modernui.graphics.drawable.Drawable
import icyllis.modernui.graphics.drawable.ImageDrawable
import icyllis.modernui.text.TextUtils
import icyllis.modernui.text.Typeface
import icyllis.modernui.view.Gravity
import icyllis.modernui.view.View
import icyllis.modernui.widget.ImageView
import icyllis.modernui.widget.LinearLayout
import icyllis.modernui.widget.TextView
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsBytes
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream

class ModCard(
    context: Context,
    val vo: ModBriefVo
): LinearLayout(context) {

    private val iconView: ImageView
    private val primaryTitleView: TextView
    private val secondaryTitleView: TextView
    private val introView: TextView

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = cardBackground()
        setPadding(context.dp(12f), context.dp(12f), context.dp(12f), context.dp(12f))
        layoutParams = linearLayoutParam(PARENT, SELF) {
            bottomMargin = context.dp(8f)
        }

        iconView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = iconBackground()
        }
        addView(iconView, linearLayoutParam(context.dp(56f), context.dp(56f)) {
            rightMargin = context.dp(16f)
        })

        val textColumn = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.START
        }
        addView(textColumn, linearLayoutParam(0, SELF) {
            weight = 1f
        })

        val titleRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        textColumn.addView(titleRow, linearLayoutParam(PARENT, SELF))

        primaryTitleView = TextView(context).apply {
            textSize = 16f
            setTextColor(MaterialColor.GRAY_900.colorValue)
            textStyle = Typeface.BOLD
            ellipsize = TextUtils.TruncateAt.END
            maxLines = 1
        }
        titleRow.addView(primaryTitleView, linearLayoutParam(0, SELF) {
            weight = 1f
        })

        secondaryTitleView = TextView(context).apply {
            textSize = 14f
            setTextColor(MaterialColor.BLUE_600.colorValue)
            textStyle = Typeface.ITALIC
            ellipsize = TextUtils.TruncateAt.END
            maxLines = 1
        }
        titleRow.addView(secondaryTitleView, linearLayoutParam(SELF, SELF) {
            leftMargin = context.dp(8f)
        })

        introView = TextView(context).apply {
            textSize = 13f
            setTextColor(MaterialColor.GRAY_700.colorValue)
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            setLineSpacing(0f, 1.2f)
        }
        textColumn.addView(introView, linearLayoutParam(PARENT, SELF) {
            topMargin = context.dp(6f)
        })

        bindData()
    }

    private fun bindData() {
        val hasChineseName = !vo.nameCn.isNullOrBlank()
        val primaryText = if (hasChineseName) vo.nameCn.trim() else vo.name.trim()

        if (hasChineseName) {
            val secondaryTrimmed = vo.name.trim()
            val shortened = shortenName(secondaryTrimmed, maxChars = 22)
            if (shortened.isNotEmpty()) {
                secondaryTitleView.text = shortened
                secondaryTitleView.visibility = View.VISIBLE
            } else {
                secondaryTitleView.visibility = View.GONE
            }
        } else {
            secondaryTitleView.visibility = View.GONE
        }

        primaryTitleView.text = primaryText.ifBlank { vo.name.trim() }
        introView.text = vo.intro.ifBlank { "暂无简介" }

        loadIcon()
    }

    private fun shortenName(text: String, maxChars: Int): String {
        val trimmed = text.trim()
        if (trimmed.length <= maxChars) return trimmed
        if (maxChars <= 1) return "…"
        return trimmed.take(maxChars - 1) + "…"
    }

    private fun loadIcon() {
        val data = vo.iconData
        if (data != null && data.isNotEmpty()) {
            val drawable = runCatching { ImageDrawable(ByteArrayInputStream(data)) }.getOrNull()
            if (drawable != null) {
                iconView.setImageDrawable(drawable)
                iconView.visibility = View.VISIBLE
                return
            }
        }

        val urlsToTry = vo.iconUrls.filter { it.isNotBlank() }.take(2)
        if (urlsToTry.isEmpty()) {
            iconView.setImageDrawable(null)
            iconView.visibility = View.GONE
            return
        }

        iconView.visibility = View.INVISIBLE
        ioScope.launch {
            val stream = fetchDrawable(urlsToTry).use {

                uiThread {
                    val drawable = ImageDrawable(it)
                    if (drawable != null) {
                        iconView.setImageDrawable(drawable)
                        iconView.visibility = View.VISIBLE
                    } else {
                        iconView.setImageDrawable(null)
                        iconView.visibility = View.GONE
                    }
                }
            }
        }
    }

    private suspend fun fetchDrawable(urls: List<String>): ByteArrayInputStream? {
        for (url in urls) {
            val response = runCatching { httpRequest { url(url) }.bodyAsBytes() }.getOrNull() ?: continue
            if (response.isEmpty()) continue
            return (ByteArrayInputStream(response))


        }
        return null
    }

    private fun cardBackground(): Drawable = drawable { canvas ->
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

    private fun iconBackground(): Drawable = drawable { canvas ->
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

}