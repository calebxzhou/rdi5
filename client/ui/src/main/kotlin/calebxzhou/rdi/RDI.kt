package calebxzhou.rdi

import calebxzhou.rdi.client.AppConfig
import calebxzhou.rdi.client.mc.startLocalServer
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
    RDI().start(_root_ide_package_.calebxzhou.rdi.client.ui.frag.TitleFragment())
}
class RDI {

    companion object {

        val DIR: File = File(System.getProperty("user.dir")).absoluteFile
    }


    init {
        lgr.info { "RDI启动中" }
        DIR.mkdir()
        lgr.info { (javaClass.protectionDomain.codeSource.location.toURI().toString()) }

    }
    fun start(fragment: calebxzhou.rdi.client.ui.frag.RFragment){
        startLocalServer()
        System.setProperty("java.awt.headless", "true")
        val mui = _root_ide_package_.calebxzhou.rdi.client.ui.RodernUI().apply {
            setTheme(R.style.Theme_Material3_Dark)
            theme.applyStyle(R.style.ThemeOverlay_Material3_Dark_Rust, true)
        }
        if (!canCreateSymlink()) {
            thread {
                _root_ide_package_.calebxzhou.rdi.client.ui.component.alertErrOs(
                    """rdi无权为mod文件创建软连接，会导致整合包玩不了，不能安装mod！
解决方法：1.以管理员身份运行rdi
 或者 2.Win+R secpol.msc 本地策略/用户权限/创建符号链接，添加当前用户，确定后重启电脑"""
                )
            }
        }
        mui.run(fragment)
        AudioManager.getInstance().close()
        System.gc()

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





