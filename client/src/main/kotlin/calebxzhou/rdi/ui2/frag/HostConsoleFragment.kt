package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.lgr
import calebxzhou.rdi.model.Host
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.server
import calebxzhou.rdi.ui2.Fonts
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.PARENT
import calebxzhou.rdi.ui2.SELF
import calebxzhou.rdi.ui2.component.confirm
import calebxzhou.rdi.ui2.fctx
import calebxzhou.rdi.ui2.go
import calebxzhou.rdi.ui2.linearLayoutParam
import calebxzhou.rdi.ui2.textView
import calebxzhou.rdi.ui2.toast
import calebxzhou.rdi.util.ioScope
import calebxzhou.rdi.ui2.uiThread
import icyllis.modernui.graphics.Color
import icyllis.modernui.graphics.drawable.ColorDrawable
import icyllis.modernui.text.SpannableStringBuilder
import icyllis.modernui.text.Spanned
import icyllis.modernui.text.style.ForegroundColorSpan
import icyllis.modernui.view.Gravity
import icyllis.modernui.view.View
import icyllis.modernui.widget.LinearLayout
import icyllis.modernui.widget.ScrollView
import icyllis.modernui.widget.TextView
import io.ktor.client.plugins.sse.SSEBufferPolicy
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.lang.System.console

class HostConsoleFragment(val host: Host) : RFragment("主机后台") {


    lateinit var console: TextView
    private lateinit var scrollView: ScrollView
    private var lastAllLines: MutableList<String> = mutableListOf()
    private var logStreamJob: Job? = null

    // Colors for different log types
    private val GOLD_COLOR = Color.rgb(255, 215, 0)     // Gold for timestamps
    private val WHITE_COLOR = Color.rgb(255, 255, 255)  // White for info
    private val YELLOW_COLOR = Color.rgb(255, 255, 0)   // Yellow for warnings
    private val RED_COLOR = Color.rgb(255, 0, 0)        // Red for errors

    init {
        bottomOptionsConfig = {
            "▶ 启动" colored MaterialColor.GREEN_900 with {
                confirm("确定要启动吗？"){
                    server.requestU("host/${host._id}/start"){
                        toast("启动指令已发送")
                    }
                }
            }
            "⟳ 重启" colored MaterialColor.BLUE_800 with {
                confirm("确定重启吗？"){
                    server.requestU("host/${host._id}/restart"){
                        toast("重启指令已发送")
                    }
                }
            }
            "⏹ 停止" colored MaterialColor.RED_900 with {
                confirm("确定停止吗？") {

                    server.requestU("host/${host._id}/stop"){
                        toast("停止指令已发送")
                    }
                }
            }
        }
    }

    init {
        contentLayoutInit = {
            gravity = Gravity.CENTER
            scrollView = ScrollView(fctx).apply {
                layoutParams = linearLayoutParam(PARENT,PARENT)
                background = ColorDrawable(Color.rgb(0, 0, 0)) // Black background
                // Leave space for bottom options row so content isn't obscured
                setPadding(0, dp(8f), 0, dp(96f))
                clipToPadding = false
                console = textView {
                    typeface = Fonts.CODE.typeface
                    layoutParams = linearLayoutParam(PARENT, SELF)
                }

            }
            addView(scrollView)
            startLogStream()

        }
    }


    private fun startLogStream() {
        logStreamJob?.cancel()
        logStreamJob = ioScope.launch {
            try {
                server.request<String>("host/${host._id}/log/200"){
                    it.data!!.lineSequence()
                        .toMutableList()
                        .reversed()
                        .map { it.trimEnd('\r') }
                        .filter { it.isNotBlank() }
                        .forEach { appendLogLine(it) }
                }
                server.sse(
                    path = "host/${host._id}/log/stream",
                    bufferPolicy = SSEBufferPolicy.LastEvents(50),
                    onEvent = { event ->
                        when (event.event) {
                            "heartbeat" -> return@sse
                            "error" -> {
                                val message = event.data?.ifBlank { null } ?: "unknown"
                                lgr.error ( "Host log stream error event: $message" )
                                toast("日志流错误: $message")
                                return@sse
                            }
                        }
                        val payload = event.data?.ifBlank { null } ?: return@sse
                        payload.lineSequence()
                            .map { it.trimEnd('\r') }
                            .filter { it.isNotBlank() }
                            .forEach { appendLogLine(it) }
                    },
                    onClosed = {
                        lgr.info("已关闭日志流")
                    },
                    onError = { throwable ->
                        lgr.error(throwable)
                    }
                )
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (t: Throwable) {
                lgr.error(t)
                uiThread {
                    toast("日志连接断开: ${t.message ?: "未知错误"}")
                }
            }
        }
    }

    private fun stopLogStream() {
        logStreamJob?.cancel()
        logStreamJob = null
    }

    override fun onDestroyView() {
        stopLogStream()
        super.onDestroyView()
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

    private fun appendLogLine(line: String) = uiThread {
        val atBottom = !scrollView.canScrollVertically(1)
        val existing = SpannableStringBuilder(console.text)
        lastAllLines.add(line)
        while (lastAllLines.size > MAX_LOG_LINES) {
            lastAllLines.removeAt(0)
            val firstBreak = existing.indexOf('\n')
            if (firstBreak >= 0) {
                existing.delete(0, firstBreak + 1)
            } else {
                existing.clear()
            }
        }
        val spanNew = displayColoredLogs(line, setDirectly = false)
        existing.append(spanNew)
        console.text = existing
        if (atBottom) {
            scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
        }
    } 

    private companion object {
        private const val MAX_LOG_LINES = 200
    }

}