package calebxzhou.rdi.client.common.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * calebxzhou @ 2025-12-15 11:49
 */
@Serializable
sealed class WsMessage
@Serializable
@SerialName("ff")
data class WsResponse<T>(val reqId: Int, val msg: T) : WsMessage()
@Serializable
@SerialName("fe")
data class WsRequest<T : WsMessage>(val reqId: Int, val msg: T) : WsMessage()
@Serializable
@SerialName("00")
class PingCommand() : WsMessage()
@Serializable
@SerialName("01")
class GetVersionIdCommand() : WsMessage()
@Serializable
@SerialName("02")
data class AddChatMsgCommand(val msg: String) : WsMessage()
@Serializable
@SerialName("03")
data class ChangeHostCommand(val newPort: Int) : WsMessage()
//todo 写ws server msg
@Serializable
@SerialName("04")
class DisconnectMsg : WsMessage()
