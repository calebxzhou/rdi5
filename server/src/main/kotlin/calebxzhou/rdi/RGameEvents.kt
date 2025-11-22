package calebxzhou.rdi

import calebxzhou.rdi.RDI.Companion.envDifficulty
import calebxzhou.rdi.RDI.Companion.envGameMode
import calebxzhou.rdi.model.CommandResultPayload
import calebxzhou.rdi.model.WsMessage
import calebxzhou.rdi.service.client
import calebxzhou.rdi.service.serdesJson
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readReason
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import net.minecraft.server.dedicated.DedicatedServer
import net.minecraft.world.Difficulty
import net.minecraft.world.level.GameRules
import net.minecraft.world.level.GameType
import org.bson.types.ObjectId
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.min
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.event.server.ServerStartedEvent
import net.neoforged.neoforge.event.server.ServerStartingEvent
import net.neoforged.neoforge.event.server.ServerStoppedEvent
import net.neoforged.neoforge.event.server.ServerStoppingEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent

@EventBusSubscriber(modid = "rdi")
class RGameEvents {
    companion object {

        private val commandRequestSerializer = WsMessage.serializer(String.serializer())
        private val commandResultSerializer = WsMessage.serializer(CommandResultPayload.serializer())
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
        fun starting(e: ServerStartingEvent){
            val server = e.server as DedicatedServer
            val difficulty = Difficulty.byId(envDifficulty)
            lgr.info("设置难度为 $difficulty")
            server.setDifficulty(difficulty,true)
            server.defaultGameType= GameType.byId(envGameMode)
            lgr.info("game type ${server.defaultGameType}")
            GameRules.visitGameRuleTypes(object : GameRules.GameRuleTypeVisitor{
                override fun <T : GameRules.Value<T>> visit(key: GameRules.Key<T>, type: GameRules.Type<T>) {
                    val gameRuleEnv = RDI.envGameRule(key.id)

                    gameRuleEnv?.let {
                        val rule = server.gameRules.getRule(key)
                        if(rule is GameRules.BooleanValue){
                            rule.set(it.toBoolean(),server)
                            lgr.info("$gameRuleEnv=$it  B")
                        }
                        if(rule is GameRules.IntegerValue){
                            rule.set(it.toInt(),server)
                            lgr.info("$gameRuleEnv=$it  I")
                        }
                    }
                }
            })
        }
        @SubscribeEvent
        @JvmStatic
        fun started(e: ServerStartedEvent){
            lgr.info("RDI启动完成启动完成启动完成启动完成")
            val server = e.server as DedicatedServer
            playSocketJob?.cancel()
            playSocketJob = ioTask{
                var hostId = System.getenv("HOST_ID")
                if(Const.DEBUG) hostId = Const.TEST_HOST_ID
                if (hostId.isNullOrBlank()) {
                    lgr.error("未找到 HOST_ID 环境变量，无法建立玩法通道")
                    if(!Const.DEBUG){
                        lgr.error("准备关服")
                        server.halt(false)
                    }
                    return@ioTask
                }

                val path = "/host/play/$hostId"
                while (currentCoroutineContext().isActive) {
                    try {
                        client.webSocket(host = if(Const.DEBUG)"127.0.0.1" else "host.docker.internal", port = 65231, path = path) {
                            lgr.info("已连接 IHQ /host/play WebSocket，hostId=$hostId")
                            for (frame in incoming) {
                                when (frame) {
                                    is Frame.Close -> {
                                        val reason = frame.readReason()
                                        lgr.warn("IHQ WebSocket 关闭: ${reason?.message ?: "无原因"}")
                                        break
                                    }

                                    is Frame.Text -> {
                                        val payload = frame.readText()
                                        handleCommandPayload(this, server, payload)
                                    }

                                    is Frame.Binary -> {
                                        lgr.warn("收到未知的二进制消息，长度=${frame.data.size}")
                                    }

                                    else -> {
                                        // ping/pong 等框架帧忽略
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

        @SubscribeEvent
        @JvmStatic
        fun stopped(e: ServerStoppedEvent){
            playSocketJob?.cancel()
            playSocketJob = null
            lgr.info("IHQ WebSocket 已在服务器停止后关闭")
        }

        private suspend fun handleCommandPayload(
            session: DefaultClientWebSocketSession,
            server: DedicatedServer,
            payload: String,
        ) {
            val message = try {
                serdesJson.decodeFromString(commandRequestSerializer, payload)
            } catch (ex: SerializationException) {
                lgr.error("解析 IHQ 指令消息失败: ${payloadPreview(payload)}", ex)
                return
            } catch (t: Throwable) {
                lgr.error("读取 IHQ 指令消息异常", t)
                return
            }

            if (message.direction != WsMessage.Direction.i2s) {
                lgr.debug("忽略非 i2s 消息: ${message.direction}")
                return
            }

            if (message.channel != WsMessage.Channel.command) {
                lgr.debug("忽略非 command 消息: ${message.channel}")
                return
            }

            val command = message.data.trim()
            if (command.isEmpty()) {
                lgr.warn("收到空命令，消息 ID=${message.id}")
                return
            }

            val (output, success) = try {
                val raw = runCommandOnServer(server, command)
                val normalized = raw.ifBlank { "<no output>" }
                lgr.info("IHQ 执行命令: $command -> $normalized")
                normalized to true
            } catch (t: Throwable) {
                lgr.error("执行 IHQ 命令失败: $command", t)
                val errorMessage = t.message?.ifBlank { t::class.java.simpleName } ?: t::class.java.simpleName
                "ERROR: $errorMessage" to false
            }

            sendCommandResult(session, message.id, command, output, success)
        }

        private fun payloadPreview(payload: String): String {
            if (payload.isEmpty()) return "<empty>"
            val end = min(payload.length, 200)
            val snippet = payload.substring(0, end)
            return if (end < payload.length) "$snippet…" else snippet
        }

        private suspend fun runCommandOnServer(server: DedicatedServer, command: String): String =
            suspendCancellableCoroutine { cont ->
                server.execute {
                    try {
                        cont.resume(server.runCommand(command))
                    } catch (t: Throwable) {
                        cont.resumeWithException(t)
                    }
                }
            }

        private suspend fun sendCommandResult(
            session: DefaultClientWebSocketSession,
            requestId: ObjectId,
            command: String,
            output: String,
            success: Boolean,
        ) {
            val response = WsMessage(
                id = requestId,
                direction = WsMessage.Direction.s2i,
                channel = WsMessage.Channel.commandResult,
                data = CommandResultPayload(
                    command = command,
                    output = output,
                    success = success,
                ),
            )

            val serialized = serdesJson.encodeToString(commandResultSerializer, response)
            try {
                session.send(Frame.Text(serialized))
            } catch (t: Throwable) {
                lgr.error("向 IHQ 发送命令结果失败: $command", t)
            }
        }

    }


}