package calebxzhou.rdi.ui.component

import calebxzhou.rdi.ui.Fonts
import calebxzhou.rdi.ui.uiThread
import icyllis.modernui.core.Context
import icyllis.modernui.graphics.Color
import icyllis.modernui.graphics.drawable.ColorDrawable
import icyllis.modernui.text.SpannableStringBuilder
import icyllis.modernui.text.Spanned
import icyllis.modernui.text.style.ForegroundColorSpan
import icyllis.modernui.view.View
import icyllis.modernui.view.ViewGroup
import icyllis.modernui.widget.ScrollView
import icyllis.modernui.widget.TextView

class Console(
    context: Context,
    private val maxLogLines: Int = 200
) : ScrollView(context) {
	private val textView = TextView(context)
	private val lines = mutableListOf<String>()
	private val buffer = SpannableStringBuilder()
	private val pending = mutableListOf<String>()
	@Volatile private var flushScheduled = false

	private val goldColor = Color.rgb(255, 215, 0)
	private val whiteColor = Color.rgb(255, 255, 255)
	private val yellowColor = Color.rgb(255, 255, 0)
	private val redColor = Color.rgb(255, 0, 0)

	init {
		background = ColorDrawable(Color.rgb(0, 0, 0))
		clipToPadding = false
		setPadding(0, dp(8f), 0, dp(8f))

		textView.typeface = Fonts.CODE.typeface
		textView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
		addView(textView)
	}

	fun append(line: String) = uiThread {
		pending += line
		if (!flushScheduled) {
			flushScheduled = true
			post { flushPending() }
		}
	}

	private fun flushPending() {
		flushScheduled = false
		if (pending.isEmpty()) return
		val atBottom = !canScrollVertically(1)
		pending.forEach { line ->
			lines.add(line)
			buffer.append(formatLine(line))
			while (lines.size > maxLogLines) {
				lines.removeAt(0)
				val firstBreak = buffer.indexOf('\n')
				if (firstBreak >= 0) {
					buffer.delete(0, firstBreak + 1)
				} else {
					buffer.clear()
				}
			}
		}
		pending.clear()
		textView.text = buffer
		if (atBottom) {
			post { fullScroll(View.FOCUS_DOWN) }
		}
	}

	private fun formatLine(line: String): SpannableStringBuilder {
		val builder = SpannableStringBuilder()
		val startPos = builder.length
		builder.append(line)
		builder.append("\n")
		when {
			line.matches(Regex("\\[\\d{2}:\\d{2}:\\d{2}\\].*")) -> {
				val closeBracketPos = line.indexOf(']')
				if (closeBracketPos > 0) {
					builder.setSpan(
						ForegroundColorSpan(goldColor),
						startPos,
						startPos + closeBracketPos + 1,
						Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
					)
					val remainingText = line.substring(closeBracketPos + 1)
					when {
						remainingText.contains("ERROR", true) ||
								remainingText.contains("SEVERE", true) ||
								remainingText.contains("FATAL", true) -> {
							builder.setSpan(
								ForegroundColorSpan(redColor),
								startPos + closeBracketPos + 1,
								startPos + line.length,
								Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
							)
						}

						remainingText.contains("WARN", true) ||
								remainingText.contains("WARNING", true) -> {
							builder.setSpan(
								ForegroundColorSpan(yellowColor),
								startPos + closeBracketPos + 1,
								startPos + line.length,
								Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
							)
						}

						else -> {
							builder.setSpan(
								ForegroundColorSpan(whiteColor),
								startPos + closeBracketPos + 1,
								startPos + line.length,
								Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
							)
						}
					}
				}
			}

			line.contains("ERROR", true) ||
					line.contains("SEVERE", true) ||
					line.contains("FATAL", true) ||
					line.contains("Exception", true) -> {
				builder.setSpan(
					ForegroundColorSpan(redColor),
					startPos,
					startPos + line.length,
					Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
				)
			}

			line.contains("WARN", true) || line.contains("WARNING", true) -> {
				builder.setSpan(
					ForegroundColorSpan(yellowColor),
					startPos,
					startPos + line.length,
					Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
				)
			}

			else -> {
				builder.setSpan(
					ForegroundColorSpan(whiteColor),
					startPos,
					startPos + line.length,
					Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
				)
			}
		}
		return builder
	}
}