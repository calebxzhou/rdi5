package calebxzhou.rdi.ui.component

import calebxzhou.mykotutils.std.humanFileSize
import calebxzhou.rdi.common.util.ioScope
import calebxzhou.rdi.lgr
import calebxzhou.rdi.ui.*
import icyllis.modernui.core.Context
import icyllis.modernui.graphics.Canvas
import icyllis.modernui.graphics.Paint
import icyllis.modernui.graphics.drawable.ColorDrawable
import icyllis.modernui.graphics.drawable.Drawable
import icyllis.modernui.graphics.drawable.ImageDrawable
import icyllis.modernui.text.TextUtils
import icyllis.modernui.view.Gravity
import icyllis.modernui.view.View
import icyllis.modernui.widget.FrameLayout
import icyllis.modernui.widget.ImageView
import icyllis.modernui.widget.LinearLayout
import icyllis.modernui.widget.TextView
import kotlinx.coroutines.launch
import org.lwjgl.util.tinyfd.TinyFileDialogs
import java.io.ByteArrayInputStream
import java.io.File
import kotlin.math.min

data class ImageSelection(
    val bytes: ByteArray,
    val file: File? = null,
    val displayName: String? = null,
) {
    val name: String
        get() = displayName ?: file?.name ?: "自定义图片"
}
class ImagePicker(context: Context) : LinearLayout(context) {


	private val previewImage: ImageView
	private val clearButton: ImageView
	private val statusLabel: TextView
	private val placeholderOverlay: View

	companion object {
		private const val MAX_DIMENSION = 512
		private const val MAX_FILE_SIZE = 256 * 1024 // 256 KB
	}

	private var currentSelection: ImageSelection? = null

	var onSelectionChanged: ((ImageSelection?) -> Unit)? = null
	var validator: ((ImageSelection) -> Boolean)? = null

	val selectedBytes: ByteArray?
		get() = currentSelection?.bytes

	val selectedFile: File?
		get() = currentSelection?.file

	val hasSelection: Boolean
		get() = currentSelection != null

	init {
		orientation = VERTICAL
		paddingDp(8)

		val previewContainer = frameLayout {
			layoutParams = linearLayoutParam(PARENT, context.dp(180f))
			background = dottedBorderDrawable()
			isClickable = true
			isFocusable = true
			setOnClickListener { openImageChooser() }
		}

		previewImage = ImageView(context).apply {
			scaleType = ImageView.ScaleType.CENTER_INSIDE
		}
		previewContainer.addView(previewImage, frameLayoutParam(PARENT, PARENT))

		placeholderOverlay = FrameLayout(context).apply {
			isClickable = false
			addView(ImageView(context).apply {
				setImageDrawable(iconDrawable("plus"))
				layoutParams = FrameLayout.LayoutParams(context.dp(48f), context.dp(48f)).apply {
					gravity = Gravity.CENTER
				}
			})
		}
		previewContainer.addView(placeholderOverlay, frameLayoutParam(PARENT, PARENT))

		clearButton = ImageView(context).apply {
			background = ColorDrawable(0xAA000000.toInt())
			setImageDrawable(iconDrawable("error"))
			paddingDp(0)
			isClickable = true
			visibility = View.GONE
			contentDescription = "清除所选图片"
			setOnClickListener {
				clearSelection()
				it.visibility = View.GONE
			}
		}
		previewContainer.addView(clearButton, frameLayoutParam(context.dp(32f), context.dp(32f)) {
			gravity = Gravity.TOP or Gravity.END
			topMargin = context.dp(6f)
			rightMargin = context.dp(6f)
		})


		statusLabel = textView("未选择图片") {
			setTextColor(MaterialColor.GRAY_300.colorValue)
			textSize = 13f
			gravity = Gravity.START
			ellipsize = TextUtils.TruncateAt.END
			maxLines = 1
			layoutParams = linearLayoutParam(PARENT, SELF) {
                topMargin = context.dp(8f)
            }
		}

		updatePlaceholder()
	}

	private fun openImageChooser() {
		val result = TinyFileDialogs.tinyfd_openFileDialog(
			"选择图片",
			null as CharSequence?,
			null,
			"Image files",
			false
		) ?: return

		val file = File(result)
		if (!file.exists() || !file.isFile) {
			toast("未找到所选文件")
			return
		}

		ioScope.launch {
			try {
				val bytes = file.readBytes()
				uiThread {
					applySelection(ImageSelection(bytes, file = file))
				}
			} catch (e: Exception) {
				lgr.warn("读取图片失败: ${e.message}")
				toast("读取图片失败: ${e.message}")
			}
		}
	}

	fun clearSelection(triggerCallback: Boolean = true) {
		currentSelection = null
		updatePlaceholder()
		if (triggerCallback) {
			onSelectionChanged?.invoke(null)
		}
	}

	fun setImage(bytes: ByteArray, displayName: String? = null, triggerCallback: Boolean = true) {
		applySelection(ImageSelection(bytes, displayName = displayName), triggerCallback)
	}

	private fun applySelection(selection: ImageSelection?, triggerCallback: Boolean = true) {
		if (selection == null) {
			clearSelection(triggerCallback)
			return
		}

        validator?.invoke(selection)?.let {
            if (!it) {
                return
            }
        }

		if (renderSelection(selection)) {
			currentSelection = selection
			if (triggerCallback) {
				onSelectionChanged?.invoke(selection)
			}
		} else {
			toast("无法加载选中的图片")
			clearSelection(triggerCallback)
		}
	}

	private fun renderSelection(selection: ImageSelection): Boolean {
		return try {
			val drawable = ImageDrawable(ByteArrayInputStream(selection.bytes))
			previewImage.setImageDrawable(drawable)
			previewImage.scaleType = ImageView.ScaleType.CENTER_CROP
			clearButton.visibility = View.VISIBLE
			placeholderOverlay.visibility = View.GONE
			statusLabel.text = selectionStatusText(selection)
			true
		} catch (e: Exception) {
			lgr.warn("解码图片失败: ${e.message}")
			false
		}
	}



	private fun selectionStatusText(selection: ImageSelection): String {
		val size = selection.bytes.size.toLong().humanFileSize
		return "${selection.name} · $size"
	}

	private fun updatePlaceholder() {
		previewImage.setImageDrawable(null)
		previewImage.scaleType = ImageView.ScaleType.CENTER_INSIDE
		clearButton.visibility = View.GONE
		placeholderOverlay.visibility = View.VISIBLE
		statusLabel.text = "未选择图片"
	}

	private fun dottedBorderDrawable(): Drawable = object : Drawable() {
		private val paint = Paint()
		private val strokeWidth = context.dp(2f).toFloat()
		private val dashLength = context.dp(12f).toFloat()
		private val gapLength = context.dp(6f).toFloat()

		override fun draw(canvas: Canvas) {
			val rect = bounds
			if (rect.width() <= 0 || rect.height() <= 0) return

			paint.setRGBA(200, 200, 200, 180)
			paint.style = Paint.Style.FILL.ordinal

			val halfStroke = strokeWidth / 2f
			val left = rect.left.toFloat() + halfStroke
			val right = rect.right.toFloat() - halfStroke
			val top = rect.top.toFloat() + halfStroke
			val bottom = rect.bottom.toFloat() - halfStroke

			var x = left
			while (x < right) {
				val endX = min(x + dashLength, right)
				canvas.drawRect(x, top - halfStroke, endX, top + halfStroke, paint)
				canvas.drawRect(x, bottom - halfStroke, endX, bottom + halfStroke, paint)
				x = endX + gapLength
			}

			var y = top
			while (y < bottom) {
				val endY = min(y + dashLength, bottom)
				canvas.drawRect(left - halfStroke, y, left + halfStroke, endY, paint)
				canvas.drawRect(right - halfStroke, y, right + halfStroke, endY, paint)
				y = endY + gapLength
			}
		}

		override fun setAlpha(alpha: Int) {}
	}


}