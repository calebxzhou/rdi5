package calebxzhou.rdi.common

import java.io.File
import java.nio.file.Paths

val DIR = File(System.getProperty("user.dir")).absoluteFile
val DL_MOD_DIR by lazy {
    System.getProperty("rdi.modDir")
        ?.let { File(it) }
        ?: DIR.resolve("dl-mods").also { it.mkdirs() }

}
object RDI {

}