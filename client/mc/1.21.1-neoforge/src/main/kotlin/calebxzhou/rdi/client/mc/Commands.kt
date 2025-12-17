package calebxzhou.rdi.client.mc

import calebxzhou.rdi.client.mc.connect.WsClient.response
import calebxzhou.rdi.client.mc.connect.WsClient.sendMessage
import calebxzhou.rdi.client.mc.connect.WsSession
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.client.gui.screens.TitleScreen

/**
 * calebxzhou @ 2025-12-15 11:49
 */
@Serializable
sealed class WsMessage {
    open suspend fun handle(session: WsSession, reqId: Int=0) {}
}

@Serializable
@SerialName("ff")
data class Response<T>(val reqId: Int, val msg: T) : WsMessage()

@Serializable
@SerialName("fe")
data class Request<T : WsMessage>(val reqId: Int, val msg: T) : WsMessage() {
    override suspend fun handle(session: WsSession, reqId: Int) {
        msg.handle(session,this.reqId)
    }
}


@Serializable
@SerialName("00")
class PingCommand() : WsMessage(){
    override suspend fun handle(session: WsSession, reqId: Int) {
        session.response(reqId,"test ok")
    }
}

@Serializable
@SerialName("01")
class GetVersionIdCommand() : WsMessage(){
    override suspend fun handle(session: WsSession, reqId: Int) {
        session.response(reqId,mc.launchedVersion)
    }
}

@Serializable
@SerialName("02")
data class AddChatMsgCommand(val msg: String) : WsMessage(){
    override suspend fun handle(session: WsSession, reqId: Int) {
        mc.addChatMessage(this.msg)
    }
}

@Serializable
@SerialName("03")
data class ChangeHostCommand(val newPort: Int) : WsMessage(){
    override suspend fun handle(session: WsSession, reqId: Int) {
        mainThread {
            mc.level?.disconnect()
            mc.disconnect()
            mc set TitleScreen()
            RDI.HOST_PORT = this.newPort
            mc.connectServer(RDI.GAME_IP)
        }
    }
}