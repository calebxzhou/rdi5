package calebxzhou.rdi.cmd

import calebxzhou.rdi.model.RBlockState
import calebxzhou.rdi.util.json
import com.ibm.icu.text.PluralRules
import com.mojang.brigadier.Command
import net.minecraft.commands.Commands
import net.minecraft.network.codec.ByteBufCodecs.registry
import net.minecraft.world.level.block.Block
import java.io.File

object DebugCommand {
    val cmd = Commands.literal("rdbg")
        .then(Commands.literal("exportBlockStates")
            .executes { ctx ->
                File("test.json").also {
                    it.createNewFile()
                    val bss = Block.BLOCK_STATE_REGISTRY.mapIndexed { id,bs ->
                        val name = bs.blockHolder.registeredName
                        val props = bs.values.map { (prop,value) ->
                            prop.name to value.toString()
                        }.toMap()
                        RBlockState(
                            name = name,
                            props = props
                        )
                    }
                    it.writeText(bss.json)
                }
                // Add reload logic here
                Command.SINGLE_SUCCESS
            })
        /*.then(Commands.literal("status")
            .executes { ctx ->
                ctx.source.sendSuccess("Current status: OK", false)
                Command.SINGLE_SUCCESS
            })*/
}