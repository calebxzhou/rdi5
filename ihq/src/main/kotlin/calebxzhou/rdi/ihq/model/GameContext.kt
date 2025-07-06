package calebxzhou.rdi.ihq.model

import calebxzhou.rdi.ihq.net.account
import calebxzhou.rdi.ihq.net.protocol.CPlayerMovePacket
import calebxzhou.rdi.ihq.service.PlayerService.sendPacket
import io.netty.channel.ChannelHandlerContext

class GameContext(
    val dimension: String = "minecraft:overworld",
    val room: Room,
    var tmpId: Byte=0,
    val net: ChannelHandlerContext
){
    fun forEachMember(handler: (Byte, RAccount) -> Unit) {
        room.onlineMembers.forEach { tmpId, member ->
            if(member._id == net.account?._id) return@forEach // 不发送给自己
            handler(tmpId,member)
        }
    }
}
