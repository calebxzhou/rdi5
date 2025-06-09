package calebxzhou.rdi.ihq.net.protocol

import calebxzhou.rdi.ihq.net.RByteBuf
import calebxzhou.rdi.ihq.net.SPacket
import calebxzhou.rdi.ihq.net.readUUID
import java.util.UUID

//移除永久生物:PersistenceRequired=1 or 是被动生物
data class SPersistEntityDelPacket(
    val entityId: UUID,
): SPacket {
    constructor(buf: RByteBuf) : this(
        entityId = buf.readUUID()
    )
    override fun handle() {
        TODO("Not yet implemented")
    }
}