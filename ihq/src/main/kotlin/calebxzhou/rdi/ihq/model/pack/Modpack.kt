package calebxzhou.rdi.ihq.model.pack

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class Modpack(
    @Contextual val _id: ObjectId = ObjectId(),
    val name: String,
    val icon: ByteArray,
    val info: String,
    val modloader: String,
    val mcVer: String,
    val versions: List<String> = arrayListOf(),
) {
    @Serializable
    data class Version(
        val name: String,
        val changelog: String,
        val mods: List<Mod>,
        val configs: List<ModConfig>,
    )
}