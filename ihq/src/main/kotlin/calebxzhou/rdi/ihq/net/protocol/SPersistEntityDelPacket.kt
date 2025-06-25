package calebxzhou.rdi.ihq.net.protocol

import calebxzhou.rdi.ihq.net.RByteBuf
import calebxzhou.rdi.ihq.net.SPacket
import calebxzhou.rdi.ihq.net.readUUID
import io.netty.channel.ChannelHandlerContext
import java.util.UUID

//移除永久生物:PersistenceRequired=1 or 是被动生物
data class SPersistEntityDelPacket(
    val entityId: UUID,
): SPacket {
    constructor(buf: RByteBuf) : this(
        entityId = buf.readUUID()
    )
    override suspend fun handle(ctx: ChannelHandlerContext) {
        TODO("Not yet implemented")
    }
}