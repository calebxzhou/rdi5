package calebxzhou.rdi.net.protocol

import calebxzhou.rdi.net.RByteBuf
import calebxzhou.rdi.net.SPacket
import calebxzhou.rdi.util.asInt
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState

class SMeBlockStateChangePacket(
    val packedChunkPos: Int,// ChunkPos.asInt
    val sectionY: Byte,
    val sectionRelativeBlockPos: Short,
    val stateID: Int,
) : SPacket {
    constructor(blockPos: BlockPos, blockState: BlockState) : this(
        ChunkPos(blockPos).asInt,
        SectionPos.blockToSectionCoord(blockPos.y).toByte(),
        SectionPos.sectionRelativePos(blockPos),
        Block.BLOCK_STATE_REGISTRY.getId(blockState)
    )

    override fun write(buf: RByteBuf) {
        buf.writeInt(packedChunkPos)
            .writeByte(sectionY)
            .writeShort(sectionRelativeBlockPos.toInt())
            .writeInt(stateID)
    }

}