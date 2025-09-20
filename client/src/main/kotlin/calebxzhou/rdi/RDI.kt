package calebxzhou.rdi

import calebxzhou.rdi.auth.LocalCredentials
import calebxzhou.rdi.auth.localCreds
import calebxzhou.rdi.model.GeoLocation
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.service.LevelService
import calebxzhou.rdi.service.Mcmod
import calebxzhou.rdi.util.ioScope
import kotlinx.coroutines.launch
import net.neoforged.fml.common.Mod
import org.apache.logging.log4j.LogManager
import java.io.File

val lgr = LogManager.getLogger("rdi")
var TOTAL_TICK_DELTA = 0f

@Mod("rdi")
class RDI {

    companion object {

        val DIR = File("rdi")
    }


    init {
        lgr.info("RDI启动中")
        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "host,connection,content-length,expect,upgrade,via")
        LevelService
        DIR.mkdir()

        Mcmod.getServerInfo()

        ioScope.launch {
            //提前启动服务器
            RServer.now.prepareRequest(true, "/room/server/start")
            if (GeoLocation.get().isp.contains("电信")) {
                lgr.info("电信节点")
                localCreds.apply { carrier=0 }.save()
            }else{
                localCreds.apply { carrier=1 }.save()
            }
        }
        /*val port = HttpUtil.getAvailablePort()
        lgr.info("local port started at $port")
        embeddedServer(CIO,host="::",port=port){
            install(StatusPages) {
                //参数不全或者有问题
                //参数不全或者有问题
                exception<ParamError> { call, cause ->
                    call.e400(cause.message)
                }
                exception<RequestError> { call, cause ->
                    call.e400(cause.message)
                }

                exception<AuthError> { call, cause ->
                    call.e401(cause.message)
                }

                //其他内部错误
                exception<Throwable> { call, cause ->
                    call.e500(cause.message)
                }
            }
            install(ContentNegotiation) {
                json(serdesJson) // Apply the custom Json configuration
            }
            install(Compression) {
                gzip()
                deflate()
            }
            routing {
                route("/dev"){
                    get("blockstates"){
                        val bss = Block.BLOCK_STATE_REGISTRY.mapIndexed { id, bs ->
                            val name = bs.blockHolder.registeredName
                            val props = bs.values.map { (prop, value) ->
                                prop.name to value.toString()
                            }.toMap()
                            RBlockState(
                                name = name,
                                props = props
                            )
                        }
                        call.ok(bss.json, ContentType.Application.Json)
                    }
                }
            }
        }.start(wait = false)*/
    }
}