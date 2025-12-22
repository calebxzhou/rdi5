package calebxzhou.rdi.master.model.pack

import calebxzhou.rdi.master.DOWNLOAD_MODS_DIR
import kotlinx.serialization.Serializable

@Serializable
data class Mod(
    val platform: String,//cf / mr / rdi
    val projectId: String,
    val slug: String,
    val fileId: String,
    val hash: String,
    val side: Side = Side.BOTH,
) {
    val fileName = "${slug}_${platform}_${hash}.jar"
    val targetPath get() = DOWNLOAD_MODS_DIR.resolve(fileName).toPath()
    enum class Side{
        CLIENT,SERVER,BOTH
    }
}