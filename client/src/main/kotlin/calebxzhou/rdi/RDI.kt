package calebxzhou.rdi

import calebxzhou.rdi.service.LevelService
import calebxzhou.rdi.service.Mcmod
import calebxzhou.rdi.service.UpdateService
import calebxzhou.rdi.util.notifyOs
import kotlinx.coroutines.runBlocking
import net.neoforged.fml.common.Mod
import org.apache.logging.log4j.LogManager
import java.io.File

val lgr = LogManager.getLogger("rdi")
var TOTAL_TICK_DELTA = 0f

@Mod("rdi")
class RDI {
    companion object {
        //mod id与中文名称map，高亮显示等用
        @JvmField
        val modIdChineseName = hashMapOf<String, String>()
    }


    init {
        lgr.info("RDI启动中")
        LevelService
        File("rdi").mkdir()

        //检查更新
        if (!(Const.DEBUG)) {
            try {
                runBlocking {


                    val modsToUpdate = UpdateService.checkUpdate(File("mods"))
                    if (modsToUpdate.isNotEmpty()) {

                        notifyOs("更新完成${modsToUpdate}")


                        UpdateService.restart()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                notifyOs("检测更新失败\n请检查网络连接")
            }
        }
        Mcmod.getServerInfo()
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