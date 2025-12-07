package calebxzhou.rdi

import calebxzhou.rdi.localserver.LOCAL_PORT
import calebxzhou.rdi.localserver.mainRoutes
import io.ktor.server.engine.*
import io.ktor.server.jetty.jakarta.*
import io.ktor.server.routing.*
import net.neoforged.fml.common.Mod
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import java.io.File

val lgr = LoggerFactory.getLogger("rdi")
val logMarker
    get() = {marker: String ->  MarkerFactory.getMarker(marker)}

class RDI {

    companion object {

        val DIR = File("rdi")
    }


    init {
        lgr.info("RDI启动中")
        DIR.mkdir()
        lgr.info("rdi核心连接码：$LOCAL_PORT")
        embeddedServer(Jetty,host="127.0.0.1",port=LOCAL_PORT){
            routing {
                mainRoutes()
            }
        }.start(wait = false)

    }
}