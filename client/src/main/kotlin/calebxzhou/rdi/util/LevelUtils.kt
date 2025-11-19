package calebxzhou.rdi.util

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunkSection

/**
 * calebxzhou @ 2025-04-14 23:13
 */

val Level.dimensionName
    get() = dimension().location().toString()
fun LevelAccessor.isAir(pos: BlockPos): Boolean {
    return getBlockState(pos).isAir
}

fun LevelAccessor.blockIs(pos: BlockPos, block: Block): Boolean {
    return getBlockState(pos).`is`(block)
}

fun LevelAccessor.blockIs(pos: BlockPos, state: BlockState): Boolean {
    return getBlockState(pos) == (state)
}

fun LevelChunkSection.forEachBlock(todo: (BlockState) -> Unit) {
    for (x in 0..15)
        for (y in 0..15)
            for (z in 0..15)
                todo(this.getBlockState(x, y, z))
}
fun Level.setBlock(pos: BlockPos, state: BlockState) {
    setBlock(pos, state, 2)
}
fun Level.setBlock(pos: BlockPos, block: Block) {
    setBlock(pos, block.defaultBlockState())
}
fun Level.setBlock(block: Block,vararg pos: BlockPos,) {
    setBlock(block.defaultBlockState(),*pos)
}
fun Level.setBlock(block: BlockState,vararg pos: BlockPos,) {
    pos.forEach {
        setBlock(it, block)
    }
}