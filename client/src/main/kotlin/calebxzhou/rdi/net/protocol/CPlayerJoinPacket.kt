package calebxzhou.rdi.net.protocol

import calebxzhou.rdi.lgr
import calebxzhou.rdi.mixin.AClientPacketListener
import calebxzhou.rdi.model.Room
import calebxzhou.rdi.net.CPacket
import calebxzhou.rdi.net.RByteBuf
import calebxzhou.rdi.net.readObjectId
import calebxzhou.rdi.net.readString
import calebxzhou.rdi.service.PlayerService
import calebxzhou.rdi.util.addChatMessage
import calebxzhou.rdi.util.ioScope
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.mcs
import kotlinx.coroutines.launch
import net.minecraft.client.multiplayer.PlayerInfo
import net.neoforged.neoforge.common.util.FakePlayer
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

        mc.addChatMessage("${name}进入了房间")
        lgr.info("${name}进入了房间 ${tempUid}=${uid}")
        ioScope.launch {
            val playerInfo = PlayerService.getPlayerInfo(uid)
            val pInfo = PlayerInfo(playerInfo.mcProfile,false)
            mc.connection?.let {
                it.listedOnlinePlayers+= pInfo
                (it as AClientPacketListener).playerInfoMap += pInfo.profile.id to pInfo
                //Room.now?.onlineMembers += tempUid to playerInfo
                mcs?.execute {
                    mcs?.overworld()?.let { level->
                        level.addFreshEntity(FakePlayer(level,playerInfo.mcProfile))
                    }

                }
            }
        }
    }
}