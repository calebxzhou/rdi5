package calebxzhou.rdi

import calebxzhou.rdi.mc.startLocalServer
import calebxzhou.rdi.ui2.RodernUI
import calebxzhou.rdi.ui2.frag.RFragment
import calebxzhou.rdi.ui2.frag.TitleFragment
import icyllis.modernui.R
import icyllis.modernui.audio.AudioManager
import io.github.oshai.kotlinlogging.KotlinLogging
import org.slf4j.MarkerFactory
import java.io.File

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
        lgr.info("RDI启动中")
        DIR.mkdir()
        lgr.info((this.javaClass.protectionDomain.codeSource.location.toURI().toString()))
        /*lgr.info("rdi核心连接码：$LOCAL_PORT")
        embeddedServer(Netty,host="127.0.0.1",port=LOCAL_PORT){
            routing {
                mainRoutes()
            }
        }.start(wait = false)*/
    }
    fun start(fragment: RFragment){
        startLocalServer()
        System.setProperty("java.awt.headless", "true")
        val mui =RodernUI().apply {
            setTheme(R.style.Theme_Material3_Dark)
            theme.applyStyle(R.style.ThemeOverlay_Material3_Dark_Rust, true)
        }
        mui.run(fragment)
        AudioManager.getInstance().close()
        System.gc()

    }
}





