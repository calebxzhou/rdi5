package calebxzhou.rdi.client.mc

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * calebxzhou @ 2025-12-15 11:49
 */
@Serializable
sealed class Command {
    open val type: String
        get() = this::class.simpleName
            ?.removeSuffix("Command")
            ?.camelToSnakeCase()
            ?: error("Command must have a name")
}
@Serializable
sealed class RequestCommand() : Command(){
    val reqId: Int=0
}

@Serializable
data class CommandResponse(val reqId: Int, val data: String) : Command()

@Serializable
data class AddChatMsgCommand(val msg: String) : Command()
@Serializable
data class ChangeHostCommand(val newPort: Int) : Command()
@Serializable
class GetVersionIdCommand() : RequestCommand()