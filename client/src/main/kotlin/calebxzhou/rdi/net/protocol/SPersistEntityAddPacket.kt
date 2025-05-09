package calebxzhou.rdi.net.protocol

import net.minecraft.nbt.CompoundTag
import java.util.UUID

//添加永久生物:PersistenceRequired=1 or 是被动生物
data class SPersistEntityAddPacket(
    val entityId: UUID,
    val data: CompoundTag
) {
}