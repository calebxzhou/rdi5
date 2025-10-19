package calebxzhou.rdi.ihq.model.pack

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

    val versions: List<@Contextual ObjectId> = arrayListOf(),
) {
    @Serializable
    data class Version(
        @Contextual val _id: ObjectId,
        @Contextual
        val modpackId: ObjectId,
        //1.0 1.1 1.2 etc 初次create作pack name用
        val name: String,
        val changelog: String,
        val mods: List<Mod>,
        val configs: List<PackFile>,
        val kjs: List<PackFile>,
    )
    @Serializable
    data class CreateDto(
        val name: String,
        val mods: List<Mod>,
        val configs: List<PackFile>,
        val kjs: List<PackFile>,
    )
}