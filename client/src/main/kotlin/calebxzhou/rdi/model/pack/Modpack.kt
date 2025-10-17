package calebxzhou.rdi.model.pack

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
class Modpack(
    @Contextual val _id: ObjectId = ObjectId(),
    val name: String,
    val icon: ByteArray?,
    @Contextual
    val authorId: ObjectId= ObjectId(),
    val info: String,
    val modloader: String,
    val mcVer: String,
    val versions: List<String> = arrayListOf(),
) {
    @Serializable
    data class Version(
        //1.0 1.1 1.2 etc 初次create作pack name用
        val name: String,
        val changelog: String,
        val mods: List<Mod>,
        val configs: List<PackFile>,
        val kjs: List<PackFile>,
    )

}