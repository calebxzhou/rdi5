package calebxzhou.rdi.ihq.model

import io.netty.channel.ChannelHandlerContext

class GameContext(
    val dimension: String = "minecraft:overworld",
    val room: Room,
    var tmpId: Byte=0,
    val net: ChannelHandlerContext
)
