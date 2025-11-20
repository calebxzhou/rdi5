package calebxzhou.rdi.ihq.model

import calebxzhou.rdi.ihq.model.pack.Mod
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId



@Serializable
data class Host(
    @Contextual
    val _id: ObjectId = ObjectId(),
    val name: String,
    @Contextual
    val ownerId: ObjectId,
    @Contextual
    val modpackId: ObjectId,
    //版本可能会重新发布 此时id会变 所以不用packVerId
    val packVer: String = "latest",
    @Contextual
    val worldId: ObjectId,
    var port: Int,
    val difficulty: Int,
    val gameMode: Int,
    val levelType: String,
    //白名单 只有成员才能进
    val whitelist: Boolean=false,
    val members: List<Member> = arrayListOf(),
    val banlist: List<@Contextual ObjectId> = arrayListOf(),
    //整合包外的附加mod
    var extraMods: List<Mod> = arrayListOf()
) {

    @Serializable
    data class Member(
        @Contextual
        val id: ObjectId,
        val role: Role
    )
}

fun Host.imageRef(): String = "${modpackId}:${this.packVer}"