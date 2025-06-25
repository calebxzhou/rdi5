package calebxzhou.rdi.ihq.net.protocol

import calebxzhou.rdi.ihq.net.RByteBuf
import calebxzhou.rdi.ihq.net.SPacket
import calebxzhou.rdi.ihq.net.readNbt
import calebxzhou.rdi.ihq.net.readString
import calebxzhou.rdi.ihq.net.readUUID
import io.netty.channel.ChannelHandlerContext
import net.benwoodworth.knbt.NbtCompound
import java.util.*

//添加永久生物:PersistenceRequired=1 or 是被动生物
data class SPersistEntityAddPacket(
    val entityId: UUID,
    val dimension: String,
    val data: NbtCompound
) : SPacket{
    constructor(buf: RByteBuf): this(buf.readUUID(), buf.readString(), buf.readNbt())
    override suspend fun handle(ctx: ChannelHandlerContext) {
        TODO("Not yet implemented")
    }
}