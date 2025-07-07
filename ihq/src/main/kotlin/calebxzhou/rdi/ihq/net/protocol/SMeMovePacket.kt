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
            ctx.account?.gameContext?.let { ctx->
                //保存状态
                ctx.pos[0]=x
                ctx.pos[1]=y
                ctx.pos[2]=z
                //转发给房间内的其他玩家
                ctx.forEachMember { tid,member ->
                    member.sendPacket(CPlayerMovePacket.Pos(tid,x, y, z))
                }
            }

        }
    }
    data class Rot(
        val yr:Float,
        val xr:Float,
    ): SPacket {
        constructor(buf: RByteBuf) : this(buf.readFloat(),buf.readFloat())

        override suspend fun handle(ctx: ChannelHandlerContext) {
            ctx.account?.gameContext?.let { ctx->
                //保存状态
                ctx.pos[3]=yr
                ctx.pos[4]=xr
                //转发给房间内的其他玩家
                ctx.forEachMember { tid, member ->
                    member.sendPacket(CPlayerMovePacket.Rot(tid,yr,xr))
                }
            }
        }
    }

}