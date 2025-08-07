package calebxzhou.rdi.service

import calebxzhou.rdi.lgr
import calebxzhou.rdi.util.chat
import calebxzhou.rdi.util.mcs
import calebxzhou.rdi.util.nickname
import net.minecraft.server.level.ServerPlayer
import java.util.*

object PlayerService  {
    val afkPlayers = hashMapOf<UUID, ServerPlayer>()

    fun onChat(player: ServerPlayer, message: String) {
        val msg = "${player.nickname}: $message"
        lgr.info(msg)
        mcs.playerList.players.forEach { it.chat(msg) }
    }
    fun isPlayerAfk(uid: UUID): Boolean {
        return afkPlayers.contains(uid)
    }


}