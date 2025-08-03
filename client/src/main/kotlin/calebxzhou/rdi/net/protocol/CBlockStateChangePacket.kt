package calebxzhou.rdi.net.protocol

import calebxzhou.rdi.net.CPacket
import calebxzhou.rdi.net.RByteBuf
import calebxzhou.rdi.util.asChunkPos
import calebxzhou.rdi.util.mcs
import calebxzhou.rdi.util.playingLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Block

class CBlockStateChangePacket(
    val packedChunkPos: Int,// ChunkPos.asInt
    val sectionY: Byte,
    val sectionRelativeBlockPos: Short,
    val stateID: Int,
) : CPacket {
    val blockPos: BlockPos
        get() {
            val chunkPos = packedChunkPos.asChunkPos
            val sectionYCoord = sectionY.toInt()
            // Unpack Y-Z-X format:
            // Y: highest 4 bits (12-15)
            // Z: middle 4 bits (4-7)
            // X: lowest 4 bits (0-3)
            val relY = (sectionRelativeBlockPos.toInt() shr 8) and 15
            val relZ = (sectionRelativeBlockPos.toInt() shr 4) and 15
            val relX = sectionRelativeBlockPos.toInt() and 15
            return BlockPos(
                chunkPos.minBlockX + relX,
                (sectionYCoord shl 4) + relY,
                chunkPos.minBlockZ + relZ
            )
        }

    constructor(buf: RByteBuf): this(
        packedChunkPos = buf.readInt(),
        sectionY = buf.readByte(),
        sectionRelativeBlockPos = buf.readShort(),
        stateID = buf.readInt()
    )

    override fun handle() {
        mcs?.playingLevel?.setBlock(blockPos, Block.stateById(stateID), 3)
    }
}