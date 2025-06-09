package calebxzhou.rdi.ihq.net.protocol

import calebxzhou.rdi.ihq.net.RByteBuf
import calebxzhou.rdi.ihq.net.SPacket
import calebxzhou.rdi.ihq.net.readObjectId
import calebxzhou.rdi.ihq.net.readString
import org.bson.types.ObjectId

//成功连接以后服务端创建timer，5秒不发登录包-断开
/**
 * 登录游戏服务器
 */
class SMeLoginPacket(
    val uid: ObjectId,
    val pwd: String
): SPacket {
    constructor(buf: RByteBuf): this(buf.readObjectId(),buf.readString())

    override fun handle() {
        TODO("Not yet implemented")
    }
}