package calebxzhou.rdi.ihq.net.protocol

import calebxzhou.rdi.ihq.net.RByteBuf
import calebxzhou.rdi.ihq.net.SPacket
import calebxzhou.rdi.ihq.net.account
import calebxzhou.rdi.ihq.net.readNbt
import calebxzhou.rdi.ihq.service.LevelService
import io.netty.channel.ChannelHandlerContext
import net.benwoodworth.knbt.NbtCompound

/**
 * 更新玩家操作的方块实体数据（）
 */
data class SMeBlockEntityUpdatePacket(
    val packedChunkPos: Int,// ChunkPos.asInt
    val sectionY: Byte,
    val sectionRelativeBlockPos: Short,//yzx
    val data: NbtCompound
): SPacket {
    constructor(buf: RByteBuf): this(buf.readInt(),buf.readByte(),buf.readShort(),buf.readNbt())
    override suspend fun handle(ctx: ChannelHandlerContext) {
        LevelService.changeBlockEntity(this,ctx.account?.gameContext ?: return)
    }

}