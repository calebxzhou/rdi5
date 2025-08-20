package calebxzhou.rdi.net.protocol

import calebxzhou.rdi.net.RByteBuf
import calebxzhou.rdi.net.SPacket
import calebxzhou.rdi.net.writeString
import net.minecraft.nbt.CompoundTag
import java.util.UUID

//添加永久生物:PersistenceRequired=1 or 是被动生物
data class SFirmEntityAddPacket(
    val entityId: UUID,
    val dimension: String,
    val data: CompoundTag
): SPacket {
    override fun write(buf: RByteBuf) {
        buf.writeUUID(entityId)
        buf.writeString(dimension)
        buf.writeNbt(data)
    }
}