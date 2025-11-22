package calebxzhou.rdi.model.pack

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
class Modpack(
    @Contextual val _id: ObjectId = ObjectId(),
    val name: String,
    @Contextual
    val authorId: ObjectId,
    val icon: ByteArray?=null,
    val info: String="暂无简介",
    val modloader: String = "neoforge",
    val mcVer: String = "1.21.1",
    val versions: List<Version> = arrayListOf(),
) {
    @Serializable
    data class Version(
        val time: Long,
        @Contextual
        val modpackId: ObjectId,
        //1.0 1.1 1.2 etc
        val name: String,
        val changelog: String,
        //构建完成状态
        val status: Status,
        val mods: List<Mod> = arrayListOf(),

        ){
    }
    enum class Status{
        FAIL,OK,BUILDING
    }
}
val List<Modpack.Version>.latest get() = maxBy { it.time }