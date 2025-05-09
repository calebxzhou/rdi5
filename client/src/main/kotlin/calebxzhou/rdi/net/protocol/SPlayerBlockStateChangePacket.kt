package calebxzhou.rdi.net.protocol

import calebxzhou.rdi.net.RByteBuf
import calebxzhou.rdi.net.SPacket
import calebxzhou.rdi.util.asInt
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.state.BlockState

class SPlayerBlockStateChangePacket(
    val chunkPos: ChunkPos,
    val sectionY: Byte,
    val localBlockPos: Int,
    val stateID: Int,
): SPacket {
    override fun write(buf: RByteBuf) {
        buf.writeInt(chunkPos.asInt)
            .writeByte(sectionY)
            .writeShort(localBlockPos)
            .writeInt(stateID)
    }

}