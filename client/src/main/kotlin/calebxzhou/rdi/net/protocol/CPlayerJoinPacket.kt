package calebxzhou.rdi.net.protocol

import calebxzhou.rdi.net.CPacket
import org.bson.types.ObjectId

data class CPlayerJoinPacket(
    val uid: ObjectId,
    val tempUid: Byte,
    val name: String,
): CPacket {
    override fun handle() {
        TODO("Not yet implemented")
    }
}