package calebxzhou.rdi.ihq.net.protocol

import calebxzhou.rdi.ihq.net.RByteBuf
import calebxzhou.rdi.ihq.net.SPacket
import calebxzhou.rdi.ihq.net.readNbt
import io.netty.channel.ChannelHandlerContext
import net.benwoodworth.knbt.NbtCompound

/**
 * 更新玩家操作的方块实体数据（）
 */
data class SPlayerBlockEntityUpdatePacket(
    val packedBlockPos: Long,
    val data: NbtCompound
): SPacket {
    constructor(buf: RByteBuf): this(buf.readLong(),buf.readNbt())
    override suspend fun handle(ctx: ChannelHandlerContext) {
        TODO("Not yet implemented")
    }

}