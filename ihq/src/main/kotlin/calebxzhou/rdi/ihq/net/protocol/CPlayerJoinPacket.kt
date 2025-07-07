package calebxzhou.rdi.ihq.net.protocol

import calebxzhou.rdi.ihq.net.CPacket
import calebxzhou.rdi.ihq.net.RByteBuf
import calebxzhou.rdi.ihq.net.writeObjectId
import calebxzhou.rdi.ihq.net.writeString
import org.bson.types.ObjectId

//其他玩家进入服务器
data class CPlayerJoinPacket(
    val uid: ObjectId,
    val tempUid: Byte,
    val name: String,
): CPacket {

    override fun write(buf: RByteBuf) {
        buf.writeObjectId(uid).writeByte(tempUid.toInt()).writeString(name)
    }
}