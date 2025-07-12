package calebxzhou.rdi.net.protocol

import calebxzhou.rdi.net.CPacket
import calebxzhou.rdi.net.RByteBuf
import calebxzhou.rdi.net.readObjectId
import calebxzhou.rdi.net.readString
import org.bson.types.ObjectId

data class CPlayerJoinPacket(
    val uid: ObjectId,
    val tempUid: Byte,
    val name: String,
): CPacket {
    constructor(buf: RByteBuf): this(
        uid = buf.readObjectId(),
        tempUid = buf.readByte(),
        name = buf.readString()
    )
    override fun handle() {
        TODO("Not yet implemented")
    }
}