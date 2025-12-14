package calebxzhou.rdi

import net.neoforged.fml.common.Mod
import org.slf4j.LoggerFactory
import org.slf4j.MarkerFactory

val lgr = LoggerFactory.getLogger("rdi")
val logMarker
    get() = { marker: String -> MarkerFactory.getMarker(marker) }

@Mod("rdi")
class RDI {

    companion object {
        @JvmField
        val IHQ_URL =
            System.getProperty("rdi.ihq.url") ?: throw IllegalArgumentException("启动方式错误：找不到服务器地址1")

        @JvmField
        val GAME_IP =
            System.getProperty("rdi.game.ip") ?: throw IllegalArgumentException("启动方式错误：找不到服务器地址2")
        @JvmField
        val HOST_NAME =
            System.getProperty("rdi.host.name") ?: throw IllegalArgumentException("启动方式错误：找不到主机名")

        @JvmField
        val HOST_PORT = System.getProperty("rdi.host.port")?.toInt() ?: throw IllegalArgumentException("启动方式错误：找不到服务器端口")

        @JvmStatic
        fun connectServer(){
            //todo
        }
    }


    init {
        lgr.info("RDI启动中")

    }
}