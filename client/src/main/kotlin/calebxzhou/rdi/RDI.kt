package calebxzhou.rdi

import calebxzhou.rdi.exception.AuthError
import calebxzhou.rdi.exception.ParamError
import calebxzhou.rdi.exception.RequestError
import calebxzhou.rdi.model.RBlockState
import calebxzhou.rdi.net.e400
import calebxzhou.rdi.net.e401
import calebxzhou.rdi.net.e500
import calebxzhou.rdi.net.ok
import calebxzhou.rdi.service.LevelService
import calebxzhou.rdi.util.json
import calebxzhou.rdi.util.serdesJson
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.routing.*
import io.netty.handler.codec.compression.StandardCompressionOptions.deflate
import io.netty.handler.codec.compression.StandardCompressionOptions.gzip
import net.minecraft.util.HttpUtil
import net.minecraft.world.level.block.Block
import net.neoforged.fml.common.Mod
import org.apache.logging.log4j.LogManager
import java.io.File

val lgr = LogManager.getLogger("rdi")
var TOTAL_TICK_DELTA = 0f
@Mod("rdi")
class RDI {

    init {
        lgr.info("RDI启动中")
        LevelService
        File("rdi").mkdir()
        val port = HttpUtil.getAvailablePort()
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
        }.start(wait = false)
    }
}