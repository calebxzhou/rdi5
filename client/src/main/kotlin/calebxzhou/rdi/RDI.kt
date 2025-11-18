package calebxzhou.rdi

import calebxzhou.rdi.localserver.LOCAL_PORT
import calebxzhou.rdi.localserver.mainRoutes
import calebxzhou.rdi.service.LevelService
import calebxzhou.rdi.service.Mcmod
import calebxzhou.rdi.util.devRoutes
import calebxzhou.rdi.util.ioScope
import io.ktor.server.engine.*
import io.ktor.server.jetty.jakarta.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import net.neoforged.fml.common.Mod
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory
import java.io.File

val lgr = LoggerFactory.getLogger("rdi")
val logMarker
    get() = {marker: String ->  MarkerFactory.getMarker(marker)}
var TOTAL_TICK_DELTA = 0f

@Mod("rdi")
class RDI {

    companion object {

        val DIR = File("rdi")
    }


    init {
        lgr.info("RDI启动中")
        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "host,connection,content-length,expect,upgrade,via")

        DIR.mkdir()

        Mcmod.getServerInfo()

        ioScope.launch {
            //提前启动服务器
           /* RServer.now.prepareRequest(true, "/room/server/start")
            if (GeoLocation.get().isp.contains("电信")) {
                lgr.info("电信节点")
                localCreds.apply { carrier=0 }.save()
            }else{
                localCreds.apply { carrier=1 }.save()
            }*/
            //HttpClient().get("https://www.baidu.com").bodyAsText().let { lgr.info(it) }
        }

        lgr.info("rdi核心连接码：$LOCAL_PORT")
        embeddedServer(Jetty,host="127.0.0.1",port=LOCAL_PORT){
            routing {
                devRoutes()
                mainRoutes()
            }
        }.start(wait = false)

    }
}