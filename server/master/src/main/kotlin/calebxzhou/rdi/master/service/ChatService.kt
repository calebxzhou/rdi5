package calebxzhou.rdi.master.service

import calebxzhou.rdi.master.DB
import calebxzhou.rdi.master.exception.AuthError
import calebxzhou.rdi.master.exception.ParamError
import calebxzhou.rdi.master.model.ChatMsg
import calebxzhou.rdi.master.net.param
import calebxzhou.rdi.master.net.response
import calebxzhou.rdi.master.net.uid
import calebxzhou.mykotutils.log.Loggers
import calebxzhou.rdi.master.util.serdesJson
import com.mongodb.client.model.Sorts
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.sse.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId
import kotlin.time.Duration.Companion.seconds

fun Route.chatRoutes() = route("/chat") {
    sse("/listen") {
        ChatService.listen(call.uid, this)
    }
    post("/send") {
        val content = param("content")
        val message = ChatService.send(uid, content)
        response(data = message)
    }
}

object ChatService {
    private const val HISTORY_LIMIT = 50
    private val dbcl = DB.getCollection<ChatMsg>("chat_msg")
    private val lgr by Loggers
    private val broadcaster = MutableSharedFlow<OutgoingMessage>(
        extraBufferCapacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private data class OutgoingMessage(
        val id: ObjectId,
        val payload: ChatMsg.Dto
    )

    suspend fun listen(uid: ObjectId, session: ServerSSESession) {
        PlayerService.getById(uid) ?: throw AuthError("无此账号")

        session.heartbeat {
            period = 15.seconds
            event = ServerSentEvent(event = "heartbeat", data = "ping")
        }

        val history = dbcl.find()
            .sort(Sorts.descending("_id"))
            .limit(HISTORY_LIMIT)
            .toList()
            .asReversed()

        for (msg in history) {
            val sender = PlayerService.getById(msg.senderId) ?: continue
            val dto = msg.toDto(sender)
            session.send(
                ServerSentEvent(
                    id = msg.id.toHexString(),
                    event = "history",
                    data = serdesJson.encodeToString(ChatMsg.Dto.serializer(), dto)
                )
            )
        }

        try {
            broadcaster.collect { outgoing ->
                session.send(
                    ServerSentEvent(
                        id = outgoing.id.toHexString(),
                        event = "message",
                        data = serdesJson.encodeToString(ChatMsg.Dto.serializer(), outgoing.payload)
                    )
                )
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (t: Throwable) {
            runCatching {
                session.send(
                    ServerSentEvent(event = "error", data = t.message ?: "unknown")
                )
            }
            throw t
        }
    }

    suspend fun send(senderId: ObjectId, content: String): ChatMsg.Dto {
        val sender = PlayerService.getById(senderId) ?: throw AuthError("无此账号")
        val normalized = content.trim()
        if (normalized.isEmpty()) throw ParamError("消息不能为空")
        if (normalized.length > 500) throw ParamError("消息过长")
        lgr.info { "${sender.name}:${content}" }
        val message = ChatMsg(sender, normalized)
        dbcl.insertOne(message)
        val dto = message.toDto(sender)
        broadcaster.emit(OutgoingMessage(message.id, dto))
        return dto
    }
}