package calebxzhou.rdi.service

import calebxzhou.rdi.Const
import calebxzhou.rdi.lgr
import calebxzhou.rdi.model.ChatMsg
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.net.server
import calebxzhou.rdi.util.error
import calebxzhou.rdi.util.addChatMessage
import calebxzhou.rdi.util.ioTask
import calebxzhou.rdi.util.gson
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.serdesJson
import io.ktor.http.HttpMethod
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay


suspend fun main() {
    RAccount.now = RAccount.TESTS[0]
    Const.DEBUG=true
    server.sse("chat/listen",
        onEvent = { event ->
            when (event.event) {
                "heartbeat" -> return@sse
                "error" -> {
                    val message = event.data?.ifBlank { null } ?: "unknown"
                    lgr.error ( "Host log stream error event: $message" )
                    return@sse
                }
            }
            val payload = event.data?.ifBlank { null } ?: return@sse
            val msg = serdesJson.decodeFromString<ChatMsg.Dto>(payload)
            lgr.info(msg.gson)
        },
        onClosed = {
            lgr.info("已关闭日志流")
        },
        onError = { throwable ->
            lgr.error(throwable)
        })
    RAccount.now = RAccount.TESTS[1]
    ioTask {
        for (i in 1000..10000){
            RAccount.now = RAccount.TESTS.random()
            server.makeRequest<Unit>("chat/send", HttpMethod.Post,mapOf("content" to "${i}")){}
            delay(10000)
        }
    }.join()
}
object ChatService {
    //0-全局聊天 1-岛内成员聊天
    var chatMode = 0
    var job: Job?=null
    @JvmStatic
    fun sendMessage(content: String){
        server.requestU("chat/send", HttpMethod.Post,mapOf("content" to content),showLoading = false){}
    }
    fun startListen(){
        job=server.sse("chat/listen",
            onEvent = { event ->
                val payload = event.data?.ifBlank { null } ?: return@sse
                val msg = serdesJson.decodeFromString<ChatMsg.Dto>(payload).let { "${it.senderName}: ${it.content}" }
                lgr.info(msg)
                mc.addChatMessage(msg)
            },
            onClosed = {
                lgr.info("已停止chat连接")
            },
            onError = { throwable ->
                mc.addChatMessage("无法连接聊天服务器，正在尝试重连")
            })
    }
    fun stopListen(){
        job?.cancel()
        job=null
    }
}