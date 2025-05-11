package calebxzhou.rdi.net.protocol

import calebxzhou.rdi.net.RByteBuf
import calebxzhou.rdi.net.SPacket
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity

/**
 * 更新玩家操作的方块实体数据（）
 */
data class SPlayerBlockEntityUpdatePacket(
    val packedBlockPos: Long,
    val data: CompoundTag
): SPacket {
    constructor(blockPos: BlockPos,level: Level,blockEntity: BlockEntity): this(blockPos.asLong(),blockEntity.saveWithFullMetadata(level.registryAccess()))

    override fun write(buf: RByteBuf) {
        buf.writeLong(packedBlockPos)
        buf.writeNbt(data)
    }
}