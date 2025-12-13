package calebxzhou.rdi.ihq.model.pack

import calebxzhou.rdi.ihq.GAME_LIBS_DIR
import calebxzhou.rdi.ihq.MODPACK_DATA_DIR
import calebxzhou.rdi.ihq.util.str
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class Modpack(
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
    val dir
        get() = MODPACK_DATA_DIR.resolve(_id.str)
    val libsDir
        get() = GAME_LIBS_DIR
            .resolve(mcVer)
            .resolve(modloader)
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
        val totalSize: Long?=0L,
        val mods: MutableList<Mod> = arrayListOf(),

        ){
        val dir
            get() = MODPACK_DATA_DIR.resolve(modpackId.str).resolve(name)
        val zip
            get() = dir.parentFile.resolve("${name}.zip")
        val clientZip
            get() = dir.parentFile.resolve("${name}-client.zip")
    }
    enum class Status{
        WAIT,FAIL,OK,BUILDING
    }
}