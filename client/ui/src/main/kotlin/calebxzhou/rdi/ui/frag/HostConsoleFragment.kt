package calebxzhou.rdi.ui.frag

import calebxzhou.rdi.common.util.ioScope
import calebxzhou.rdi.lgr
import calebxzhou.rdi.net.server
import calebxzhou.rdi.ui.*
import calebxzhou.rdi.ui.component.confirm
import icyllis.modernui.view.Gravity
import io.ktor.client.plugins.sse.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.bson.types.ObjectId
import calebxzhou.rdi.ui.component.Console

class HostConsoleFragment(val hostId: ObjectId) : RFragment("主机后台") {

    private lateinit var console: Console
    private var logStreamJob: Job? = null
    private var logStreamSseJob: Job? = null
    private var isFragmentActive = true

    init {
        titleViewInit = {
            quickOptions {
                "▶ 启动" colored MaterialColor.GREEN_900 with {
                    confirm("确定要启动吗？"){
                        server.requestU("host/${hostId}/start"){
                            toast("启动指令已发送")
                        }
                    }
                }

                "⟳ 重启" colored MaterialColor.BLUE_800 with {
                    confirm("确定重启吗？"){
                        server.requestU("host/${hostId}/restart"){
                            toast("重启指令已发送")
                        }
                    }
                }
                "⏹ 停止" colored MaterialColor.RED_900 with {
                    confirm("确定停止吗？") {

                        server.requestU("host/${hostId}/stop"){
                            toast("停止指令已发送")
                        }
                    }
                }
            }
        }
    }

    init {
        contentViewInit = {
            gravity = Gravity.CENTER
            console = Console(fctx)
            console.layoutParams = linearLayoutParam(PARENT, PARENT)
            addView(console)
            startLogStream()

        }
    }


    private fun startLogStream() {
        logStreamJob?.cancel()
        logStreamJob = null
        logStreamSseJob?.cancel()
        logStreamSseJob = null
        isFragmentActive = true
        logStreamJob = ioScope.launch {
            try {
                server.request<String>("host/${hostId}/log/200"){
                    it.data!!.lineSequence()
                        .toMutableList()
                        .reversed()
                        .map { it.trimEnd('\r') }
                        .filter { it.isNotBlank() }
                        .forEach { console.append(it) }
                }
                logStreamSseJob = server.sse(
                    path = "host/${hostId}/log/stream",
                    bufferPolicy = SSEBufferPolicy.LastEvents(50),
                    onEvent = { event ->
                        if (!isFragmentActive) return@sse
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
                            .forEach { console.append(it) }
                    },
                    onClosed = {
                        lgr.info("已关闭日志流")
                    },
                    onError = { throwable ->
                        lgr.error { throwable }
                    }
                )
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (t: Throwable) {
                lgr.error { t }
                if (isFragmentActive) {
                    uiThread {
                        toast("日志连接断开: ${t.message ?: "未知错误"}")
                    }
                }
            } finally {
                logStreamSseJob = null
            }
        }
    }

    private fun stopLogStream() {
        isFragmentActive = false
        logStreamSseJob?.cancel()
        logStreamSseJob = null
        logStreamJob?.cancel()
        logStreamJob = null
    }

    override fun close() {
        stopLogStream()
        super.close()
    }

}