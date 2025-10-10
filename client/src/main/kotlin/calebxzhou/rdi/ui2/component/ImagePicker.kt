package calebxzhou.rdi.ui2.component

import calebxzhou.rdi.lgr
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.PARENT
import calebxzhou.rdi.ui2.SELF
import calebxzhou.rdi.ui2.dp
import calebxzhou.rdi.ui2.frameLayout
import calebxzhou.rdi.ui2.frameLayoutParam
import calebxzhou.rdi.ui2.iconDrawable
import calebxzhou.rdi.ui2.linearLayout
import calebxzhou.rdi.ui2.linearLayoutParam
import calebxzhou.rdi.ui2.paddingDp
import calebxzhou.rdi.ui2.textView
import calebxzhou.rdi.ui2.toast
import calebxzhou.rdi.ui2.uiThread
import calebxzhou.rdi.util.ioScope
import icyllis.modernui.core.Context
import icyllis.modernui.graphics.drawable.ColorDrawable
import icyllis.modernui.graphics.drawable.ImageDrawable
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
import java.util.Locale

class ImagePicker(context: Context) : LinearLayout(context) {

	data class Selection(
		val bytes: ByteArray,
		val file: File? = null,
		val displayName: String? = null,
	) {
		val name: String
			get() = displayName ?: file?.name ?: "自定义图片"
	}

	private val placeholderDrawable = iconDrawable("camera")
	private val previewImage: ImageView
	private val placeholderLabel: TextView
	private val fileNameLabel: TextView
	private val chooseButton: RButton
	private val clearButton: RButton

	private var currentSelection: Selection? = null

	var onSelectionChanged: ((Selection?) -> Unit)? = null

	val selectedBytes: ByteArray?
		get() = currentSelection?.bytes

	val selectedFile: File?
		get() = currentSelection?.file

	val hasSelection: Boolean
		get() = currentSelection != null

	init {
		orientation = VERTICAL
		paddingDp(12, 12, 12, 12)
		background = ColorDrawable(0x22181818)

		val previewContainer = frameLayout {
			layoutParams = linearLayoutParam(PARENT, context.dp(180f)) {
				bottomMargin = context.dp(8f)
			}
			background = ColorDrawable(0x33222222)
		}

		previewImage = ImageView(context).apply {
			scaleType = ImageView.ScaleType.CENTER
		}
		previewContainer.addView(previewImage, frameLayoutParam(PARENT, PARENT))

		placeholderLabel = TextView(context).apply {
			text = "点击下方按钮选择图片"
			setTextColor(MaterialColor.GRAY_300.colorValue)
			textSize = 14f
			gravity = Gravity.CENTER
			background = null
		}
		previewContainer.addView(placeholderLabel, frameLayoutParam(PARENT, PARENT) {
			gravity = Gravity.CENTER
		})

		fileNameLabel = textView("未选择图片") {
			setTextColor(MaterialColor.GRAY_200.colorValue)
			textSize = 13f
			gravity = Gravity.START
			layoutParams = linearLayoutParam(PARENT, SELF) {
				bottomMargin = context.dp(8f)
			}
		}

		val buttonRow = linearLayout {
			orientation = LinearLayout.HORIZONTAL
			layoutParams = linearLayoutParam(PARENT, SELF)
		}

		chooseButton = RButton(context, color = MaterialColor.BLUE_600) {
			openImageChooser()
		}.apply {
			text = "选择图片"
		}
		buttonRow.addView(chooseButton, linearLayoutParam(0, SELF) {
			weight = 1f
		})

		clearButton = RButton(context, color = MaterialColor.GRAY_700) {
			clearSelection()
		}.apply {
			text = "清除"
		}
		buttonRow.addView(clearButton, linearLayoutParam(0, SELF) {
			weight = 1f
			leftMargin = context.dp(8f)
		})

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
					applySelection(Selection(bytes, file = file))
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
		applySelection(Selection(bytes, displayName = displayName), triggerCallback)
	}

	private fun applySelection(selection: Selection?, triggerCallback: Boolean = true) {
		if (selection == null) {
			clearSelection(triggerCallback)
			return
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

	private fun renderSelection(selection: Selection): Boolean {
		return try {
			val drawable = ImageDrawable(ByteArrayInputStream(selection.bytes))
			previewImage.setImageDrawable(drawable)
			previewImage.scaleType = ImageView.ScaleType.CENTER_CROP
			placeholderLabel.visibility = View.GONE
			fileNameLabel.text = "${selection.name} (${formatSize(selection.bytes.size)})"
			true
		} catch (e: Exception) {
			lgr.warn("解码图片失败: ${e.message}")
			false
		}
	}

	private fun updatePlaceholder() {
		previewImage.setImageDrawable(placeholderDrawable)
		previewImage.scaleType = ImageView.ScaleType.CENTER
		placeholderLabel.visibility = View.VISIBLE
		placeholderLabel.text = "点击下方按钮选择图片"
		fileNameLabel.text = "未选择图片"
	}

	private fun formatSize(bytes: Int): String {
		if (bytes < 1024) return "$bytes B"
		val kb = bytes / 1024.0
		if (kb < 1024) return String.format(Locale.getDefault(), "%.1f KB", kb)
		val mb = kb / 1024.0
		if (mb < 1024) return String.format(Locale.getDefault(), "%.2f MB", mb)
		val gb = mb / 1024.0
		return String.format(Locale.getDefault(), "%.2f GB", gb)
	}
}