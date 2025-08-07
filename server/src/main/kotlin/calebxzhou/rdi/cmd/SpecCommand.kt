package calebxzhou.rdi.cmd

import calebxzhou.rdi.model.RBlockState
import calebxzhou.rdi.util.*
import com.mojang.brigadier.Command
import kotlinx.coroutines.launch
import net.minecraft.commands.Commands
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.Block
import java.io.File

object SpecCommand {
    val cmd = Commands.literal("spec")
                .executes { ctx ->
                    ctx.source.player?.let {
                        if (it.gameMode.gameModeForPlayer == GameType.SPECTATOR) {
                            it.level().sharedSpawnPos.apply {
                                it.teleportTo(x.toDouble(), y.toDouble(), z.toDouble())
                            }
                            it.setGameMode(GameType.SURVIVAL)
                        }else{
                            it.setGameMode(GameType.SPECTATOR)
                        }
                    }
                 1
                }
    /*.then(Commands.literal("status")
        .executes { ctx ->
            ctx.source.sendSuccess("Current status: OK", false)
            Command.SINGLE_SUCCESS
        })*/
}