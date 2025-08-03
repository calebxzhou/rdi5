package calebxzhou.rdi.net.protocol

import calebxzhou.rdi.lgr
import calebxzhou.rdi.mixin.AClientPacketListener
import calebxzhou.rdi.model.Room
import calebxzhou.rdi.net.CPacket
import calebxzhou.rdi.net.RByteBuf
import calebxzhou.rdi.util.addChatMessage
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.mcs

data class CPlayerLeavePacket(
    val tempUid: Byte
): CPacket {
    constructor(buf: RByteBuf): this(
        tempUid = buf.readByte()
    )
    override fun handle() {
        Room.now?.let { room->
            room.onlineMembers[tempUid]?.let { account->
                mc.addChatMessage("${account.name}退出了房间")
                lgr.info("${account} quit the room")
                mc.connection?.let {
                    it.listedOnlinePlayers -= account.mcPlayerInfo
                    (it as AClientPacketListener).playerInfoMap -= account.mcProfile.id
                    room.onlineMembers -= tempUid
                    mcs?.execute {
                        mcs?.allLevels?.forEach { level ->
                            level.getEntity(account.mcProfile.id)?.kill()
                        }
                    }
                }
            }
        }
    }
}