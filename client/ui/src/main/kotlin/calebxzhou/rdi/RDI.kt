package calebxzhou.rdi

import calebxzhou.rdi.mc.startLocalServer
import calebxzhou.rdi.ui.RodernUI
import calebxzhou.rdi.ui.component.alertErrOs
import calebxzhou.rdi.ui.component.alertWarn
import calebxzhou.rdi.ui.frag.RFragment
import calebxzhou.rdi.ui.frag.TitleFragment
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
    RDI().start(TitleFragment())
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
    fun start(fragment: RFragment){
        startLocalServer()
        System.setProperty("java.awt.headless", "true")
        val mui =RodernUI().apply {
            setTheme(R.style.Theme_Material3_Dark)
            theme.applyStyle(R.style.ThemeOverlay_Material3_Dark_Rust, true)
        }
        if (!canCreateSymlink()) {
            thread {
                alertErrOs(
                    """无法为mod文件创建软连接，可能会导致rdi核心无法正常更新，整合包无法安装mod
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





