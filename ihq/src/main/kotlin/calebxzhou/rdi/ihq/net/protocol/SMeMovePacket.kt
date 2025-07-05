package calebxzhou.rdi.ihq.net.protocol

import calebxzhou.rdi.ihq.net.RByteBuf
import calebxzhou.rdi.ihq.net.SPacket
import calebxzhou.rdi.ihq.net.account
import calebxzhou.rdi.ihq.service.PlayerService.sendPacket
import io.netty.channel.ChannelHandlerContext


class SMeMovePacket()  {
    data class Pos(
        val x: Float,
        val y: Float,
        val z: Float,
    ): SPacket {
        constructor(buf: RByteBuf) : this(buf.readFloat(),buf.readFloat(),buf.readFloat())

        override suspend fun handle(ctx: ChannelHandlerContext) {
            //转发给房间内的其他玩家
            ctx.account?.gameContext?.room?.onlineMembers?.forEach { (tempId, member) ->
                if(member._id == ctx.account?._id) return@forEach // 不发送给自己
                member.sendPacket(CPlayerMovePacket.Pos(tempId.toByte(),x, y, z))
            }
        }
    }
    data class Rot(
        val yr:Float,
        val xr:Float,
    ): SPacket {
        constructor(buf: RByteBuf) : this(buf.readFloat(),buf.readFloat())

        override suspend fun handle(ctx: ChannelHandlerContext) {
            //转发给房间内的其他玩家
            ctx.account?.gameContext?.room?.onlineMembers?.forEach { (tempId, member) ->
                if(member._id == ctx.account?._id) return@forEach // 不发送给自己
                member.sendPacket(CPlayerMovePacket.Rot(tempId.toByte(),yr,xr))
            }
        }
    }

}