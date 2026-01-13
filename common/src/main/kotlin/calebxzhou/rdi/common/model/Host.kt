package calebxzhou.rdi.common.model

import calebxzhou.rdi.model.Role
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId


@Serializable
data class Host(
    @Contextual
    val _id: ObjectId = ObjectId(),
    val name: String,
    val intro:String="暂无简介",
    val icon: ByteArray? = null,
    @Contextual
    val ownerId: ObjectId,
    @Contextual
    val modpackId: ObjectId,
    //版本可能会重新发布 此时id会变 所以不用packVerId
    var packVer: String = "latest",
    @Contextual
    val worldId: ObjectId?=null,
    var port: Int,
    val difficulty: Int,
    val gameMode: Int,
    val levelType: String,
    val gameRules: MutableMap<String,String> = mutableMapOf(),
    //白名单 只有成员才能进
    val whitelist: Boolean=false,
    //允许作弊（全op）
    val allowCheats: Boolean=false,
    val members: List<Member> = arrayListOf(),
    val banlist: List<@Contextual ObjectId> = arrayListOf(),
    //整合包外的附加mod
    var extraMods: List<Mod> = arrayListOf()
) {
    companion object{
        var portNow : Int =0
    }
    @Serializable
    data class Member(
        @Contextual
        val id: ObjectId,
        val role: Role
    )
    @Serializable
    data class Vo(
        @Contextual
        val _id: ObjectId = ObjectId(),
        val name: String,
        val intro: String = "暂无简介",
        val icon: ByteArray? = null,
        val ownerName: String,
        val modpackName: String,
        val packVer: String,
        var port: Int,
    ){
        companion object{
            val TEST = Vo(name="啊实打实大苏打实打实的", intro = "都是大大实打实大苏打实打实的得分风格风格风格非官方", ownerName = "点点滴滴都是", modpackName = "测试测试测试测试测试", packVer = "1.0.0", port = 55555)
        }
    }

}
