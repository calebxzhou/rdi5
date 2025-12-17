package calebxzhou.rdi.mc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * calebxzhou @ 2025-12-15 11:49
 */
@Serializable
sealed class LocalWsMessage
@Serializable
sealed class RequestCommand() : LocalWsMessage(){
    var reqId: Int=0
}
@Serializable
@SerialName("ff")
data class Response(val reqId: Int, val data: String): LocalWsMessage()

@Serializable
@SerialName("00")
class PingCommand() : RequestCommand()
@Serializable
@SerialName("01")
class GetVersionIdCommand() : RequestCommand()
@Serializable
@SerialName("02")
data class AddChatMsgCommand(val msg: String) : LocalWsMessage()
@Serializable
@SerialName("03")
data class ChangeHostCommand(val newPort: Int) : LocalWsMessage()