package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.exception.ParamError
import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.model.RAccount
import calebxzhou.rdi.ihq.net.idExtractor
import calebxzhou.rdi.ihq.net.uid
import com.mongodb.client.model.Filters.eq
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.util.AttributeKey
import kotlinx.coroutines.flow.firstOrNull
import org.bson.types.ObjectId

data class PlayerContext(
    val player: RAccount,
)
val PlayerPlugin = createRouteScopedPlugin("PlayerPlugin") {
    val contextKey = AttributeKey<PlayerContext>("PlayerContext")

    onCall { call ->
        val player = PlayerService.getById(call.uid)
            ?: throw RequestError("用户不存在")
        call.attributes[contextKey] = PlayerContext(player)
    }
}

val ApplicationCall.playerContext: PlayerContext
    get() = attributes[AttributeKey("PlayerContext")]