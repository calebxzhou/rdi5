package calebxzhou.rdi.service

import calebxzhou.mykotutils.log.Loggers
import calebxzhou.rdi.client.common.protocol.AddChatMsgCommand
import calebxzhou.rdi.common.model.ChatMsg
import calebxzhou.rdi.common.serdesJson
import calebxzhou.rdi.mc.mcSession
import calebxzhou.rdi.mc.send
import calebxzhou.rdi.net.server
import io.ktor.http.*
import kotlinx.coroutines.Job

object ChatService {
    private val lgr by Loggers
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
                lgr.info { msg }
                mcSession?.send(AddChatMsgCommand(msg))
            },
            onClosed = {
                lgr.info { "已停止chat连接" }
            },
            onError = { throwable ->
            })
    }
    fun stopListen(){
        job?.cancel()
        job=null
    }
}