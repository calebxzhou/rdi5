package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.DB
import calebxzhou.rdi.ihq.model.RChatMessage
import calebxzhou.rdi.ihq.service.PlayerService.getById
import calebxzhou.rdi.ihq.util.e400
import calebxzhou.rdi.ihq.util.e404
import calebxzhou.rdi.ihq.util.got
import calebxzhou.rdi.ihq.util.ok
import calebxzhou.rdi.ihq.util.uid
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.receiveParameters

object ChatService {
    val dbcl = DB.getCollection<RChatMessage>("chat_record")
    suspend fun chat(call: ApplicationCall) {
        val params = call.receiveParameters()
        val mode = params got "mode"
        val content = params got "content"
        getById(call.uid)?.let { sender ->
            when (mode) {
                //全服
                "0" -> {
                  //  rconPost("broadcast ${call.uid} $content")
                    dbcl.insertOne(RChatMessage(senderId = call.uid, content = content))
                    call.ok()
                }
                //成员间
                "1" -> {
                    RoomService.getJoinedRoom(call.uid)?.let {
                      //  rconPost("tell ${call.uid} ${it.members.map { it.id }.joinToString(",")} $content")
                        dbcl.insertOne(RChatMessage(senderId = call.uid, content = content))
                    }
                    call.ok()
                }

                else -> call.e400()
            }
        } ?: call.e404()
    }

}