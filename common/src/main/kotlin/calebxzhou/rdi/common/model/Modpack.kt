package calebxzhou.rdi.common.model

import jogamp.opengl.util.pngj.ImageLineHelper.pack
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
class Modpack(
    @Contextual val _id: ObjectId = ObjectId(),
    val name: String,
    @Contextual
    val authorId: ObjectId,
    val iconUrl: String? = null,
    val info: String="暂无简介",
    val modloader: ModLoader,
    val mcVer: McVersion,
    val sourceUrl : String ? = null,
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
        val totalSize: Long?=0L,
        val status: Status,
        val mods: MutableList<Mod> = arrayListOf(),
        ){
    }
    @Serializable
    data class BriefVo(
        @Contextual
        val id: ObjectId = ObjectId(),
        val name: String = "未知整合包",
        @Contextual
        val authorId: ObjectId = ObjectId(),
        val authorName: String = "",
        val mcVer: McVersion = McVersion.V211,
        val modloader: ModLoader = ModLoader.neoforge,
        val modCount: Int = 0,
        val fileSize: Long = 0L,
        val icon: String? = null,
        val info: String = "暂无简介",
    )

    @Serializable
    data class DetailVo(
        @Contextual
        val _id: ObjectId,
        val name: String,
        @Contextual
        val authorId: ObjectId,
        val authorName: String = "",
        val modCount: Int,
        val icon: String? = null,
        val info: String = "暂无简介",
        val modloader: ModLoader,
        val mcVer: McVersion,
        val versions: List<Version> = arrayListOf(),
    )
    @Serializable
    data class OptionsDto(
        val name: String? = null,
        val iconUrl: String? = null,
        val info: String? = null,
        val sourceUrl: String? = null
    )
    enum class Status{
        FAIL,OK,BUILDING,WAIT,
    }
}
val List<Modpack.Version>.latest get() = maxBy { it.time }
