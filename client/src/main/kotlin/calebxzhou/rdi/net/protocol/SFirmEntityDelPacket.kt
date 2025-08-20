package calebxzhou.rdi.net.protocol

import calebxzhou.rdi.net.RByteBuf
import calebxzhou.rdi.net.SPacket
import java.util.UUID

//移除永久生物:PersistenceRequired=1 or 是被动生物
data class SFirmEntityDelPacket(
    val entityId: UUID,
) : SPacket{
    override fun write(buf: RByteBuf) {
        buf.writeUUID(entityId)
    }
}