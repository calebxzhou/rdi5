package calebxzhou.rdi

import calebxzhou.rdi.service.client
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readReason
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.server.ServerStartedEvent
import net.neoforged.neoforge.event.server.ServerStoppingEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent

@EventBusSubscriber(modid = "rdi")
class RGameEvents {
    companion object {

        private var playSocketJob: Job? = null


        @SubscribeEvent
        @JvmStatic
        fun tickStart(e: ServerTickEvent.Pre){
            RDI.tickTime1 = System.currentTimeMillis()
        }
        @SubscribeEvent
        @JvmStatic
        fun tickEnd(e: ServerTickEvent.Post){
            RDI.tickTime2 = System.currentTimeMillis()
            RDI.tickDelta = RDI.tickTime2 - RDI.tickTime1
        }
        @SubscribeEvent
        @JvmStatic
        fun stopping(e: ServerStoppingEvent){
            playSocketJob?.cancel()
            playSocketJob = null
        }
        @SubscribeEvent
        @JvmStatic
        fun started(e: ServerStartedEvent){
            lgr.info("====启动完成启动完成启动完成启动完成====")
            playSocketJob?.cancel()
            playSocketJob = ioTask{
                val hostId = System.getenv("HOST_ID")
                if (hostId.isNullOrBlank()) {
                    lgr.warn("未找到 HOST_ID 环境变量，无法建立玩法通道")
                    return@ioTask
                }

                val path = "/host/play/$hostId"
                while (currentCoroutineContext().isActive) {
                    try {
                        client.webSocket(host = "host.docker.internal", port = 65231, path = path) {
                            lgr.info("已连接 IHQ /host/play WebSocket，hostId=$hostId")
                            for (frame in incoming) {
                                when (frame) {
                                    is Frame.Close -> {
                                        val reason = frame.readReason()
                                        lgr.warn("IHQ WebSocket 关闭: ${reason?.message ?: "无原因"}")
                                        break
                                    }

                                    else -> {
                                        // 暂时忽略来自 IHQ 的消息，后续再处理
                                    }
                                }
                            }
                        }
                    } catch (ce: CancellationException) {
                        lgr.info("IHQ WebSocket 协程已取消，准备断开连接")
                        break
                    } catch (t: Throwable) {
                        lgr.error("连接 IHQ WebSocket 失败，将在 5 秒后重试", t)
                        delay(5_000)
                        continue
                    }

                    // WebSocket 正常退出，稍后重连
                    delay(5_000)
                }
            }
        }

    }


}