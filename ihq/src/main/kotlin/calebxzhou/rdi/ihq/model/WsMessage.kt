package calebxzhou.rdi.ihq.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId


@Serializable
data class WsMessage<T>(
    @Contextual
    val id: ObjectId = ObjectId(),
    val direction: Direction = Direction.i2s,
    val channel: Channel,
    val data: T,
) {

    @Serializable
    enum class Direction {
        // ihq -> server
        i2s,

        // server -> ihq
        s2i,
    }

    @Serializable
    enum class Channel {
        player,
        block,
        command,
        commandResult,
        system,
        chat,
    }
}
