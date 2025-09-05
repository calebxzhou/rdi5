package calebxzhou.rdi.ihq.model

import calebxzhou.rdi.ihq.net.account
import io.netty.channel.ChannelHandlerContext

class GameContext(
    var dimension: String = "minecraft:overworld",
    // 0=x 1=y 2=z 3=yr(yaw) 4=xr(pitch)
    val pos: FloatArray = floatArrayOf(0f,0f,0f,0f,0f),
    val room: Room,
    var tmpId: Byte=0,
    val net: ChannelHandlerContext
){
    fun forEachMember(handler: (Byte, RAccount) -> Unit) {
        /*room.onlineMembers.forEach { tmpId, member ->
            if(member._id == net.account?._id) return@forEach // 不发送给自己
            handler(tmpId,member)
        }*/
    }
}
