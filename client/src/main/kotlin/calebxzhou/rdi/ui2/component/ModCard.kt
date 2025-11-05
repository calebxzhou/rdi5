package calebxzhou.rdi.ui2.component

import calebxzhou.rdi.model.ModBriefVo
import calebxzhou.rdi.net.httpRequest
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
    val vo: ModBriefVo,
    enableSelect: Boolean = true,
    simpleMode: Boolean = true
): LinearLayout(context) {

    private val iconView: ImageView
    private val primaryTitleView: TextView
    private val secondaryTitleView: TextView
    private val introView: TextView
    private var cardSelected = false
    var enableSelect: Boolean = enableSelect
        set(value) {
            field = value
            isClickable = value
            isFocusable = value
            if (!value) {
                setSelectedState(false)
            }
        }
    var simpleMode: Boolean = simpleMode
        set(value) {
            if (field == value) return
            field = value
            applyMode()
        }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(context.dp(12f), context.dp(6f), context.dp(12f), context.dp(6f))
        layoutParams = linearLayoutParam(PARENT, SELF) {
            bottomMargin = context.dp(8f)
        }
        this.enableSelect = enableSelect
        updateSelectionBackground()

        iconView = ImageView(context).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = iconBackground()
        }
        addView(iconView, linearLayoutParam(context.dp(if (simpleMode) 14f else 56f), context.dp(if (simpleMode) 14f else 56f)) {
            rightMargin = context.dp(if (simpleMode) 8f else 16f)
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

            setTextColor(MaterialColor.GRAY_900.colorValue)
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
        applyMode()
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

    fun setSelectedState(isSelected: Boolean) {
        if (cardSelected == isSelected) return
        cardSelected = isSelected
        updateSelectionBackground()
    }

    fun toggleSelectedState(): Boolean {
        if (!enableSelect) {
            return cardSelected
        }
        setSelectedState(!cardSelected)
        return cardSelected
    }

    fun isCardSelected(): Boolean = cardSelected

    private fun updateSelectionBackground() {
        background = cardBackground(cardSelected)
    }

    private fun applyMode() {
        updateIconLayout()
        updateTextContent()
    }

    private fun updateIconLayout() {
        val iconLp = iconView.layoutParams as? LinearLayout.LayoutParams ?: return
        val size = context.dp(if (simpleMode) 28f else 56f)
        iconLp.width = size
        iconLp.height = size
        iconLp.rightMargin = context.dp(if (simpleMode) 8f else 16f)
        iconView.layoutParams = iconLp
    }

    private fun updateTextContent() {
        tooltipText = vo.intro
        val hasChineseName = !vo.nameCn.isNullOrBlank()
        if (simpleMode) {
            val primaryText = vo.nameCn?.trim().orEmpty().ifBlank { vo.name.trim() }

            primaryTitleView.apply {
                    textSize = 12f
                    textStyle = Typeface.BOLD
                    text = primaryText.ifBlank { vo.name.trim() }
            }
            secondaryTitleView.visibility = View.GONE
            introView.visibility = View.GONE
        } else {
            val primaryText = if (hasChineseName) vo.nameCn.trim() else vo.name.trim()
            primaryTitleView.apply {
                textSize = 16f
                textStyle = Typeface.BOLD
                text = primaryText.ifBlank { vo.name.trim() }
            }
            introView.text = vo.intro.ifBlank { "暂无简介" }
            introView.visibility = View.VISIBLE


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
        }
    }

    private fun cardBackground(isSelected: Boolean): Drawable = drawable { canvas ->
        val fillPaint = Paint.obtain()
        fillPaint.setRGBA(255, 255, 255, 235)
        fillPaint.style = Paint.Style.FILL.ordinal
        val radius = context.dp(16f).toFloat()
        canvas.drawRoundRect(
            bounds.left.toFloat(),
            bounds.top.toFloat(),
            bounds.right.toFloat(),
            bounds.bottom.toFloat(),
            radius,
            fillPaint
        )
        fillPaint.recycle()

        if (isSelected) {
            val strokePaint = Paint.obtain()
            strokePaint.setRGBA(100, 220, 100, 255)
            strokePaint.style = Paint.Style.STROKE.ordinal
            strokePaint.strokeWidth = context.dp(3f).toFloat()
            canvas.drawRoundRect(
                bounds.left.toFloat() + strokePaint.strokeWidth / 2,
                bounds.top.toFloat() + strokePaint.strokeWidth / 2,
                bounds.right.toFloat() - strokePaint.strokeWidth / 2,
                bounds.bottom.toFloat() - strokePaint.strokeWidth / 2,
                radius,
                strokePaint
            )
            strokePaint.recycle()
        }
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