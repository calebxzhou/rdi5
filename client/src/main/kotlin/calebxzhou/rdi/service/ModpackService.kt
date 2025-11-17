package calebxzhou.rdi.service

import calebxzhou.rdi.ui2.pointerBuffer
import org.lwjgl.util.tinyfd.TinyFileDialogs

val selectModpackFile
    get() = TinyFileDialogs.tinyfd_openFileDialog(
    "选择整合包 (ZIP)",
    "C:/Users/${System.getProperty("user.name")}/Downloads",
    ("*.zip").pointerBuffer,
    "CurseForge整合包 (*.zip)",
    false
)
object ModpackService {
    fun parseLocal(zipPath: String){

    }
}