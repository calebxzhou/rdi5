package calebxzhou.rdi.event

import calebxzhou.rdi.util.dimensionName
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunk
import net.neoforged.bus.api.Event

class BlockStateChangedEvent(
    val level: Level,
    val chunk: LevelChunk,
    val blockPos: BlockPos,
    val blockState: BlockState,
    val isMoving: Boolean
) : Event(){
    override fun toString(): String {
        return "方块状态改变：${level.dimensionName} ${blockPos} ${blockState.block}"
    }
}