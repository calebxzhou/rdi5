package calebxzhou.rdi.cmd

import ca.weblite.objc.RuntimeUtils.msg
import calebxzhou.rdi.util.mcComp
import calebxzhou.rdi.util.mcs
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.MessageArgument

object SpeakCommand {
    val cmd = Commands.literal("speak")
        .then(Commands.argument("msg", MessageArgument.message()).executes {
            val msg = MessageArgument.getMessage(it, "msg")
            val newMsg = it.source.displayName.string +": "+ msg.string
            mcs.playerList.broadcastSystemMessage(newMsg.mcComp,false)

            1
        })
}