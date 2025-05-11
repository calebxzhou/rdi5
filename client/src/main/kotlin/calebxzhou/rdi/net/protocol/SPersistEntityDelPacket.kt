package calebxzhou.rdi.net.protocol

import java.util.UUID

//移除永久生物:PersistenceRequired=1 or 是被动生物
data class SPersistEntityDelPacket(
    val entityId: UUID,
) {
}