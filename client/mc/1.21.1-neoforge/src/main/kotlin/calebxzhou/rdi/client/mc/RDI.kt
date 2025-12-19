package calebxzhou.rdi.client.mc

import calebxzhou.rdi.client.mc.connect.WsClient.connectCore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import net.neoforged.fml.common.Mod
import org.slf4j.LoggerFactory

val lgr = LoggerFactory.getLogger("rdi")
val scope = CoroutineScope(Dispatchers.IO)
val serdesJson = Json {
    ignoreUnknownKeys = true  // Good for forward compatibility
    classDiscriminator = "type"  // Uses the @SerialName as discriminator
}
inline val <reified T> T.json: String
    get() = serdesJson.encodeToString<T>(this)

@Mod("rdi")
class RDI {
    companion object {
        @JvmField val IHQ_URL = System.getProperty("rdi.ihq.url") ?: throw IllegalArgumentException("启动方式错误：找不到服务器地址1")
        @JvmField val GAME_IP = System.getProperty("rdi.game.ip") ?: throw IllegalArgumentException("启动方式错误：找不到服务器地址2")
        @JvmField val HOST_NAME = System.getProperty("rdi.host.name") ?: throw IllegalArgumentException("启动方式错误：找不到主机名")
        @JvmField var HOST_PORT = System.getProperty("rdi.host.port")?.toInt() ?: throw IllegalArgumentException("启动方式错误：找不到服务器端口")


    }


    init {
        lgr.info("RDI启动中")
        scope.launch {
            runCatching {
                connectCore()
            }.onFailure { error ->
                lgr.error("WebSocket connection failed", error)
            }
        }
    }
}