package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.body
import calebxzhou.rdi.ui2.bottomOptions
import calebxzhou.rdi.ui2.fctx
import calebxzhou.rdi.ui2.iconButton
import calebxzhou.rdi.util.ioScope
import calebxzhou.rdi.util.uiThread
import icyllis.modernui.graphics.Color
import icyllis.modernui.graphics.drawable.ColorDrawable
import icyllis.modernui.text.SpannableStringBuilder
import icyllis.modernui.text.Spanned
import icyllis.modernui.text.Typeface
import icyllis.modernui.text.style.ForegroundColorSpan
import icyllis.modernui.view.Gravity
import icyllis.modernui.view.View
import icyllis.modernui.widget.LinearLayout
import icyllis.modernui.widget.ScrollView
import icyllis.modernui.widget.TextView
import kotlinx.coroutines.launch

class ServerFragment(val server: RServer) : RFragment("服务端") {
    lateinit var console: TextView
    private lateinit var scrollView: ScrollView
    private var currentPage = 0
    private var isLoading = false

    // Colors for different log types
    private val GOLD_COLOR = Color.rgb(255, 215, 0)     // Gold for timestamps
    private val WHITE_COLOR = Color.rgb(255, 255, 255)  // White for info
    private val YELLOW_COLOR = Color.rgb(255, 255, 0)   // Yellow for warnings
    private val RED_COLOR = Color.rgb(255, 0, 0)        // Red for errors

    override fun initContent() {
        contentLayout.apply {
            gravity = Gravity.CENTER
            scrollView = ScrollView(fctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                background = ColorDrawable(Color.rgb(0, 0, 0)) // Black background
                console = TextView(fctx).apply {
                    Typeface.getSystemFont("Sarasa Mono SC").let {
                       // if(it != Typeface.){
                            typeface = it
                       // }
                    }

                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }
                addView(console)

                // Add scroll listener to detect when user scrolls to top
                setOnScrollChangeListener { _, _, scrollY, _, _ ->
                    if (scrollY == 0 && !isLoading) {
                        loadMoreLogs()
                    }
                }
            }

            bottomOptions {
                iconButton("play","启动")
                iconButton("stop","停止")
                iconButton("zip","升级/重装")
            }
            addView(scrollView)
        }

        // Load initial logs
        loadLogs()
    }

    private fun loadLogs() {
        isLoading = true
        ioScope.launch {
            try {
                val log = getLog(currentPage)
                uiThread {
                    // Reverse the log order to show earliest logs at top
                    val reversedLog = reverseLogOrder(log)
                    displayColoredLogs(reversedLog)
                    isLoading = false

                    // Scroll to bottom after logs are displayed
                    scrollView.post {
                        scrollView.fullScroll(View.FOCUS_DOWN)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                uiThread {
                    console.text = "Error loading logs: ${e.message}"
                    isLoading = false
                }
            }
        }
    }

    private fun loadMoreLogs() {
        isLoading = true
        currentPage++

        ioScope.launch {
            try {
                val oldLogs = getLog(currentPage)
                uiThread {
                    // Remember scroll height before adding content
                    val prevHeight = console.height
                    val scrollY = scrollView.scrollY

                    // Prepend old logs to existing content
                    val currentContent = console.text
                    val reversedOldLogs = reverseLogOrder(oldLogs)
                    val newContent = displayColoredLogs(reversedOldLogs, false)
                    val builder = SpannableStringBuilder()
                    builder.append(newContent)
                    builder.append(currentContent)
                    console.text = builder

                    // Restore scroll position after the new content is added
                    scrollView.post {
                        // After layout pass, measure the height difference and adjust scroll position
                        val heightDiff = console.height - prevHeight
                        if (heightDiff > 0) {
                            scrollView.scrollTo(0, scrollY + heightDiff)
                        }
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                currentPage-- // Revert page increment on failure
                uiThread {
                    isLoading = false
                }
            }
        }
    }

    // Function to reverse the log order, so earlier logs appear at top
    private fun reverseLogOrder(log: String): String {
        return log.split("\n").reversed().joinToString("\n")
    }

    private fun displayColoredLogs(logText: String, setDirectly: Boolean = true): SpannableStringBuilder {
        val builder = SpannableStringBuilder()

        // Split logs into lines
        val lines = logText.split("\n")

        for (line in lines) {
            val startPos = builder.length
            builder.append(line)
            builder.append("\n")

            // Apply colors based on log content
            when {
                // Color timestamps (assumed to be at the beginning of the line, like [HH:MM:SS])
                line.matches(Regex("\\[\\d{2}:\\d{2}:\\d{2}\\].*")) -> {
                    // Find the closing bracket of timestamp
                    val closeBracketPos = line.indexOf(']')
                    if (closeBracketPos > 0) {
                        builder.setSpan(
                            ForegroundColorSpan(GOLD_COLOR),
                            startPos,
                            startPos + closeBracketPos + 1,
                            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        // Color the rest of the line based on log level
                        val remainingText = line.substring(closeBracketPos + 1)
                        when {
                            remainingText.contains("ERROR", ignoreCase = true) ||
                            remainingText.contains("SEVERE", ignoreCase = true) ||
                            remainingText.contains("FATAL", ignoreCase = true) -> {
                                builder.setSpan(
                                    ForegroundColorSpan(RED_COLOR),
                                    startPos + closeBracketPos + 1,
                                    startPos + line.length,
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                            }
                            remainingText.contains("WARN", ignoreCase = true) ||
                            remainingText.contains("WARNING", ignoreCase = true) -> {
                                builder.setSpan(
                                    ForegroundColorSpan(YELLOW_COLOR),
                                    startPos + closeBracketPos + 1,
                                    startPos + line.length,
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                            }
                            else -> {
                                builder.setSpan(
                                    ForegroundColorSpan(WHITE_COLOR),
                                    startPos + closeBracketPos + 1,
                                    startPos + line.length,
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                            }
                        }
                    }
                }
                // Lines without timestamps but with error indicators
                line.contains("ERROR", ignoreCase = true) ||
                line.contains("SEVERE", ignoreCase = true) ||
                line.contains("FATAL", ignoreCase = true) ||
                line.contains("Exception", ignoreCase = true) -> {
                    builder.setSpan(
                        ForegroundColorSpan(RED_COLOR),
                        startPos,
                        startPos + line.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                // Lines without timestamps but with warning indicators
                line.contains("WARN", ignoreCase = true) ||
                line.contains("WARNING", ignoreCase = true) -> {
                    builder.setSpan(
                        ForegroundColorSpan(YELLOW_COLOR),
                        startPos,
                        startPos + line.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                // Default to white for other content
                else -> {
                    builder.setSpan(
                        ForegroundColorSpan(WHITE_COLOR),
                        startPos,
                        startPos + line.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        }

        if (setDirectly) {
            console.text = builder
        }

        return builder
    }

    suspend fun getLog(page: Int = 0): String {
        val resp = server.prepareRequest(false, "room/log?page=$page")
        return resp.body
    }
}