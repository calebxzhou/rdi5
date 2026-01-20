package calebxzhou.rdi

import calebxzhou.rdi.client.AppConfig
import calebxzhou.rdi.client.ui.RodernUI
import calebxzhou.rdi.client.ui.component.alertErrOs
import icyllis.modernui.R
import icyllis.modernui.audio.AudioManager
import io.github.oshai.kotlinlogging.KotlinLogging
import org.slf4j.MarkerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.concurrent.thread

val CONF = AppConfig.load()
val lgr = KotlinLogging.logger {  }
val logMarker
    get() = {marker: String ->  MarkerFactory.getMarker(marker)}
fun main(){
    //redirect new ui
    calebxzhou.rdi.client.ui2.main()
    //RDIClient.start(_root_ide_package_.calebxzhou.rdi.client.ui.frag.TitleFragment())
}
object RDIClient {


        val DIR: File = File(System.getProperty("user.dir")).absoluteFile



    init {
        lgr.info { "RDI启动中" }
        DIR.mkdir()
        lgr.info { (javaClass.protectionDomain.codeSource.location.toURI().toString()) }

    }

    }

    private fun canCreateSymlink(): Boolean {
        val target = runCatching { Files.createTempFile(DIR.toPath(), "symlink-test-target", ".tmp") }.getOrNull()
            ?: return true
        val link: Path = DIR.toPath().resolve("symlink-test-link-${System.currentTimeMillis()}")
        return runCatching {
            Files.deleteIfExists(link)
            Files.createSymbolicLink(link, target)
            true
        }.onFailure { err ->
            lgr.warn(err) { "符号链接创建失败" }
        }.getOrElse { false }.also {
            runCatching { Files.deleteIfExists(link) }
            runCatching { Files.deleteIfExists(target) }
        }
    }
}





