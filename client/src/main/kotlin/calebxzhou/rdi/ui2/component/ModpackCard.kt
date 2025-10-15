package calebxzhou.rdi.ui2.component

import calebxzhou.rdi.model.pack.Modpack
import calebxzhou.rdi.ui2.Fonts
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.PARENT
import calebxzhou.rdi.ui2.SELF
import calebxzhou.rdi.ui2.drawable
import calebxzhou.rdi.ui2.dp
import calebxzhou.rdi.ui2.horizontal
import calebxzhou.rdi.ui2.iconDrawable
import calebxzhou.rdi.ui2.imageView
import calebxzhou.rdi.ui2.leadingIcon
import calebxzhou.rdi.ui2.linearLayout
import calebxzhou.rdi.ui2.linearLayoutParam
import calebxzhou.rdi.ui2.textView
import calebxzhou.rdi.ui2.vertical
import icyllis.modernui.core.Context
import icyllis.modernui.graphics.Paint
import icyllis.modernui.graphics.drawable.ColorDrawable
import icyllis.modernui.graphics.drawable.ImageDrawable
import icyllis.modernui.text.TextUtils
import icyllis.modernui.view.Gravity
import icyllis.modernui.view.ViewGroup
import icyllis.modernui.widget.FrameLayout
import icyllis.modernui.widget.ImageView
import icyllis.modernui.widget.LinearLayout
import icyllis.modernui.widget.TextView
import jdk.internal.vm.ThreadContainers.container
import java.io.ByteArrayInputStream

class ModpackCard(
	context: Context,
	private val modpack: Modpack,
) : FrameLayout(context) {

	private lateinit var iconView: ImageView
	private lateinit var nameView: TextView
	private lateinit var authorView: TextView
	private lateinit var descriptionView: TextView
	private lateinit var tagRow: LinearLayout

	init {
		background = cardBackground()
		foreground = ColorDrawable(0x33FFFFFF)
		setPadding(context.dp(16f), context.dp(16f), context.dp(16f), context.dp(16f))
		layoutParams = linearLayoutParam(PARENT, SELF) {
			bottomMargin = context.dp(12f)
		}
		isClickable = true
		isFocusable = true

        linearLayout {
            horizontal()
            gravity = Gravity.CENTER_VERTICAL
            iconView = imageView {
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = linearLayoutParam(context.dp(64f), context.dp(64f)) {
                    rightMargin = context.dp(16f)
                }
                background = circleBackground()
                setPadding(context.dp(4f), context.dp(4f), context.dp(4f), context.dp(4f))
            }
            //right column
            linearLayout {
                vertical()
                gravity = Gravity.START
                layoutParams = linearLayoutParam(0,SELF) { weight = 1f }
                nameView = textView {
                    text = modpack.name
                    textSize = 18f
                    setTextColor(MaterialColor.WHITE.colorValue)
                }
                authorView = textView {
                    textSize = 14f
                    setTextColor(MaterialColor.GRAY_300.colorValue)
                }
                descriptionView = textView {
                    textSize = 13f
                    setTextColor(0xCCFFFFFF.toInt())
                    maxLines = 2
                    ellipsize = TextUtils.TruncateAt.END

                    setLineSpacing(0f, 1.2f)
                }
                tagRow = LinearLayout(context).apply {
                    horizontal()
                    gravity = Gravity.START
                }
            }
        }

		bindData()
	}

	private fun bindData() {
		// Icon
		val iconBytes = modpack.icon
		val packIcon = if (iconBytes.isNotEmpty()) {
			try {
				ImageDrawable(ByteArrayInputStream(iconBytes))
			} catch (_: Exception) {
				null
			}
		} else null
		iconView.setImageDrawable(packIcon ?: iconDrawable("mcmod"))

		// Author and description heuristic based on info text
		val infoLines = modpack.info.lines().map { it.trim() }.filter { it.isNotEmpty() }
		authorView.text =  "by todo"

		val description = when {
			infoLines.isEmpty() -> "暂无简介"
			infoLines.first().startsWith("by ", ignoreCase = true) -> infoLines.getOrNull(1) ?: "暂无简介"
			else -> infoLines.first()
		}
		descriptionView.text = description

		// Tags
		tagRow.removeAllViews()
		if (modpack.versions.isNotEmpty()) {
			addTag(null, "${modpack.versions.size} versions")
		}
	}

	private fun addTag(icon: String?, label: String) {
		if (label.isBlank()) return

		val corner = context.dp(12f).toFloat()

		val tv = TextView(context).apply {
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
		tagRow.addView(tv, linearLayoutParam(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT) {
			if (tagRow.childCount > 0) leftMargin = context.dp(8f)
			topMargin = context.dp(8f)
		})
	}

	private fun circleBackground() = drawable { canvas ->
		val paint = Paint.obtain()
		paint.setRGBA(50, 50, 50, 255)
		paint.style = Paint.Style.FILL.ordinal
		val radius = Math.min(bounds.width(), bounds.height()) / 2f
		canvas.drawCircle(bounds.exactCenterX(), bounds.exactCenterY(), radius, paint)
		paint.recycle()
	}

	private fun cardBackground() = drawable { canvas ->
		val paint = Paint.obtain()
		paint.setRGBA(30, 30, 30, 230)
		paint.style = Paint.Style.FILL.ordinal
		val radius = context.dp(18f).toFloat()
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