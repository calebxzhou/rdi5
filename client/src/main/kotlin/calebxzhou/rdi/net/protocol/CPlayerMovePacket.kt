package calebxzhou.rdi.net.protocol

import calebxzhou.rdi.model.Room
import calebxzhou.rdi.net.CPacket
import calebxzhou.rdi.net.RByteBuf
import calebxzhou.rdi.util.mcs

class CPlayerMovePacket {
    data class Pos(
        val tempUid: Byte,
        val x: Float,
        val y: Float,
        val z: Float,
    ): CPacket{
        constructor(buf: RByteBuf): this(buf.readByte(),buf.readFloat(),buf.readFloat(),buf.readFloat())
        override fun handle() {
            Room.now?.let { room ->
                mcs?.execute {
                    mcs?.allLevels?.forEach { level ->
                       /* room.onlineMembers[tempUid]?.mcProfile?.let { level.getEntity(it.id) }?.setPos(x.toDouble(), y.toDouble(),
                            z.toDouble()
                        )*/
                    }
                }
            }
        }

    }
    data class Rot(
        val tempUid: Byte,
        val yr:Float,
        val xr:Float,
    ): CPacket{
        constructor(buf: RByteBuf): this(buf.readByte(),buf.readFloat(),buf.readFloat())
        override fun handle() {
            Room.now?.let { room ->
                mcs?.execute {
                    mcs?.allLevels?.forEach { level ->
                       /* room.onlineMembers[tempUid]?.mcProfile?.let { level.getEntity(it.id) }?.let { ent->
                            ent.yRot = yr
                            ent.xRot = xr
                        }*/


                    }
                }
            }
        }

    }
}
