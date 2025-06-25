package calebxzhou.rdi.ihq.model

import io.netty.channel.ChannelHandlerContext
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

@Serializable
data class RAccount(
    @Contextual
    val _id: ObjectId = ObjectId(),
    val name: String,
    val pwd: String,
    val qq: String,
    val score: Int = 0,
    val cloth: Cloth = Cloth(),
) {
    @Transient
    var networkContext: ChannelHandlerContext? = null

    @Serializable
    data class Cloth(
        val isSlim: Boolean = true,
        var skin: String = "https://littleskin.cn/textures/526fe866ed25a7ee1cf894b81a2199aaa03f139803623a25a793f6ae57e22f02",
        var cape: String? = null
    )

    @Serializable
    data class Dto(
        @Contextual
        val id: ObjectId,
        val name: String,
        val cloth: Cloth
    )

    val dto
        get() = Dto(_id, name, cloth)
}