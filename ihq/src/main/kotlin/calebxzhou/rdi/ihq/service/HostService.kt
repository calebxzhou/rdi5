package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.DB
import calebxzhou.rdi.ihq.DEFAULT_MODPACK_ID
import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.model.Host
import calebxzhou.rdi.ihq.model.HostStatus
import calebxzhou.rdi.ihq.model.WsMessage
import calebxzhou.rdi.ihq.model.imageRef
import calebxzhou.rdi.ihq.net.ok
import calebxzhou.rdi.ihq.net.param
import calebxzhou.rdi.ihq.net.paramNull
import calebxzhou.rdi.ihq.net.response
import calebxzhou.rdi.ihq.net.uid
import calebxzhou.rdi.ihq.service.TeamService.addHost
import calebxzhou.rdi.ihq.service.TeamService.delHost
import calebxzhou.rdi.ihq.util.str
import calebxzhou.rdi.ihq.util.serdesJson
import calebxzhou.rdi.ihq.lgr
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.webSocket
import io.ktor.sse.ServerSentEvent
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.serializer
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.minutes
import org.bson.types.ObjectId

// ---------- Routing DSL (mirrors teamRoutes style) ----------
fun Route.hostRoutes() = route("/host") {
    get("/{hostId}/status") {
        response(
            data = HostService.getServerStatus(ObjectId(param("hostId"))).toString()
        )
    }
    post("/{hostId}/start") {
        HostService.start(uid, ObjectId(param("hostId")))
        ok()
    }
    post("/{hostId}/stop") {
        HostService.stop(uid, ObjectId(param("hostId")))
        ok()
    }
    post("/{hostId}/command") {
        val hostId = ObjectId(param("hostId"))
        val command = param("command")
        HostService.sendCommand(uid, hostId, command)
        ok()
    }
    post("/{hostId}/restart") {
        HostService.restart(uid, ObjectId(param("hostId")))
        ok()
    }
    post("/{hostId}/update") {

        HostService.update(
            uid,
            ObjectId(param("hostId")),
            paramNull("packVer")
        )
        ok()
    }
    post("/") {
        HostService.create(
            uid,
            //todo 内测阶段只支持默认整合包
            //ObjectId(param("modpackId")),
            DEFAULT_MODPACK_ID,
            //todo 内测阶段只支持最新版
            "latest",
            //param("packVer"),
            paramNull("worldId")?.let { ObjectId(it) }
        )
        ok()
    }
    delete("/{hostId}") {
        HostService.delete(uid, ObjectId(param("hostId")))
        ok()
    }
    get("/{hostId}/log/{lines}") {
        val hostId = ObjectId(param("hostId"))
        val lines = param("lines").toInt()
        response(data = HostService.getLog(uid, hostId, paramNull("startLine")?.toInt() ?: 0, lines))
    }
    sse("/{hostId}/log/stream") {
        val hostId = ObjectId(call.param("hostId"))
        HostService.listenLogs(call.uid, hostId, this)
    }
    get("/") {
        TeamService.getJoinedTeam(uid)
            ?.let { HostService.listByTeam(it._id) }
            ?.let { response(data = it) }
            ?: response(data = emptyList<Host>())
    }
    /* todo 以后再支持 for 自由选择host
    get("/all") {
        response(data = HostService.dbcl.find().map { it._id.toString() to it.name }.toList())
    }*/
}

fun Route.hostPlayRoutes() = route("/host") {
    webSocket("/play/{hostId}") {
        val rawHostId = call.param("hostId")

        val hostId = runCatching { ObjectId(rawHostId) }.getOrElse {
            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "host无效"))
            return@webSocket
        }

        val host = HostService.getById(hostId)
        if (host == null) {
            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "未知主机"))
            return@webSocket
        }

        if (!HostService.registerPlayableSession(hostId, this)) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "重复连接"))
            return@webSocket
        }

        try {
            for (frame in incoming) {
                // 服务器端暂不处理来自 mod 的消息
            }
        } finally {
            HostService.unregisterPlayableSession(hostId, this)
        }
    }
}

object HostService {

    val dbcl = DB.getCollection<Host>("host")

    private const val PORT_START = 50000

    private const val PORT_END_EXCLUSIVE = 60000
    private const val SHUTDOWN_THRESHOLD = 10

    private val idleMonitorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var idleMonitorJob: Job? = null

    private data class HostState(
        var shutFlag: Int = 0,
        var session: DefaultWebSocketServerSession? = null
    )

    private val hostStates = ConcurrentHashMap<ObjectId, HostState>()

    fun registerPlayableSession(hostId: ObjectId, session: DefaultWebSocketServerSession): Boolean {
        val state = hostStates.computeIfAbsent(hostId) { HostState() }
        val previous = state.session
        if (previous !== null && previous !== session) {
            previous.launch {
                runCatching { previous.close(CloseReason(CloseReason.Codes.NORMAL, "新的连接建立")) }
            }
        }
        state.session = session
        lgr.info { "Host $hostId gameplay 通道已连接" }
        return true
    }

    fun unregisterPlayableSession(hostId: ObjectId, session: DefaultWebSocketServerSession) {
        hostStates[hostId]?.let { state ->
            if (state.session === session) {
                state.session = null
                lgr.info { "Host $hostId gameplay 通道已断开" }
                if (state.shutFlag <= 0) {
                    hostStates.remove(hostId, state)
                }
                idleMonitorScope.launch {
                    delay(3.seconds)
                    val currentSession = hostStates[hostId]?.session
                    if (currentSession != null) {
                        lgr.info { "Host $hostId 已在断开后重新连接，跳过自动停止" }
                        return@launch
                    }

                    runCatching { DockerService.stop(hostId.str) }
                        .onSuccess {
                            lgr.info { "Host $hostId 容器因通道断开已停止" }
                        }
                        .onFailure { error ->
                            if (error is RequestError && error.message == "早就停了") {
                                lgr.info { "Host $hostId 容器已处于停止状态" }
                            } else {
                                lgr.warn(error) { "Host $hostId 通道断开后停止容器失败: ${error.message}" }
                            }
                        }
                }
            }
        }
    }

    // Pick a port in [50000, 60000) that isn't used by any existing room
    private suspend fun allocateRoomPort(): Int {
        val used = dbcl.find().map { it.port }.toList().toSet()
        val candidates = (PORT_START until PORT_END_EXCLUSIVE).asSequence()
            .filter { it !in used }
            .toList()
        if (candidates.isEmpty()) {
            throw RequestError("没有可用端口，请联系管理员")
        }
        return candidates.random()
    }

    suspend fun getRunnings(): List<Host> = withContext(Dispatchers.IO) {
        val runningIds = DockerService.listContainers(includeStopped = false)
            .mapNotNull { container ->
                val containerName = container.names?.firstOrNull()?.removePrefix("/") ?: return@mapNotNull null
                runCatching { ObjectId(containerName) }.getOrNull()
            }
            .distinct()
        if (runningIds.isEmpty()) emptyList() else dbcl.find(`in`("_id", runningIds)).toList()
    }

    suspend fun getIdles(): List<Host> {
        val result = mutableListOf<Host>()
        for (host in getRunnings()) {
            if (host.getOnlinePlayers() == 0) {
                result += host
            }
        }
        return result
    }

    fun startIdleMonitor() {
        if (idleMonitorJob?.isActive == true) return
        idleMonitorJob = idleMonitorScope.launch {
            while (isActive) {
                try {
                    runIdleMonitorTick()
                } catch (cancel: CancellationException) {
                    throw cancel
                } catch (t: Throwable) {
                    lgr.warn(t) { "Idle monitor tick failed: ${t.message}" }
                }
                delay(1.minutes)
            }
        }
    }

    fun stopIdleMonitor() {
        idleMonitorJob?.cancel()
        idleMonitorJob = null
        hostStates.values.forEach { it.shutFlag = 0 }
    }

    private suspend fun runIdleMonitorTick(forceStop: Boolean = false) {
        val runningHosts = try {
            getRunnings()
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (t: Throwable) {
            lgr.warn(t) { "Failed to fetch running hosts: ${t.message}" }
            return
        }
        if (runningHosts.isEmpty()) return

        for (host in runningHosts) {
            val onlinePlayers = try {
                host.getOnlinePlayers()
            } catch (cancel: CancellationException) {
                throw cancel
            }

            if (onlinePlayers <= 0) {
                if (forceStop) {
                    clearShutFlag(host._id)
                    stopHost(host, "forced idle shutdown")
                    continue
                }

                val current = hostStates[host._id]?.shutFlag ?: 0
                val newFlag = current + 1
                updateShutFlag(host._id, newFlag)
                if (newFlag >= SHUTDOWN_THRESHOLD) {
                    stopHost(host, "idle for $newFlag consecutive minutes")
                    clearShutFlag(host._id)
                }
            } else {
                clearShutFlag(host._id)
            }
        }
    }

    private fun updateShutFlag(hostId: ObjectId, value: Int) {
        if (value <= 0) {
            clearShutFlag(hostId)
        } else {
            val state = hostStates.computeIfAbsent(hostId) { HostState() }
            state.shutFlag = value
            lgr.info { "upd shut flag ${hostId} ${value}" }
        }
    }

    private fun clearShutFlag(hostId: ObjectId) {
        hostStates[hostId]?.let { state ->
            state.shutFlag = 0
            if (state.session == null) {
                hostStates.remove(hostId, state)
            }
        }
        lgr.info { "clear shut flag $hostId" }
    }

    private fun stopHost(host: Host, reason: String) {
        runCatching {
            DockerService.stop(host._id.str)
            lgr.info { "Stopped host ${host._id} ($reason)" }
        }.onFailure {
            lgr.warn(it) { "Failed to stop host ${host._id}: ${it.message}" }
        }
        clearShutFlag(host._id)
    }

    // ---------- Core Logic (no ApplicationCall side-effects) ----------
    suspend fun Host.getOnlinePlayers(): Int = try {
        McServerPinger.ping(this.port).players?.online ?: 0
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (t: Throwable) {
        lgr.warn(t) { "Failed to ping host ${this._id}: ${t.message}" }
        0
    }

    suspend fun create(uid: ObjectId, modpackId: ObjectId, packVer: String, worldId: ObjectId?) {
        val player = PlayerService.getById(uid) ?: throw RequestError("玩家未注册")
        val team = TeamService.getOwn(uid) ?: throw RequestError("你不是队长")
        if (team.hosts().size > 1) throw RequestError("内测阶段只能有一个主机")
        val world = worldId?.let {
            if (findByWorld(it) != null) throw RequestError("此存档已被其他主机占用")

            WorldService.getById(it) ?: throw RequestError("无此存档")
        } ?: WorldService.create(uid, null, modpackId)
        if (world.teamId != team._id) throw RequestError("存档不属于你的团队")

        val port = allocateRoomPort()
        val host = Host(
            name = team.name + "的主机" + (team.hosts().size + 1),
            teamId = team._id,
            modpackId = modpackId,
            worldId = world._id,
            port = port,
            packVer = packVer
        )
        dbcl.insertOne(host)
        team.addHost(host._id)
        DockerService.createContainer(port, host._id.str, world._id.str, host.imageRef())
        DockerService.start(host._id.str)
        clearShutFlag(host._id)
    }

    suspend fun delete(uid: ObjectId, hostId: ObjectId) {
        val team = TeamService.getOwn(uid) ?: throw RequestError("你不是队长")
        if (!team.hasHost(hostId)) throw RequestError("此主机不属于你的团队")
        val host = getById(hostId) ?: throw RequestError("无此主机")
        dbcl.deleteOne(eq("_id", hostId))
        team.delHost(hostId)
        DockerService.deleteContainer(hostId.str)
        clearShutFlag(hostId)
    }


    suspend fun update(uid: ObjectId, hostId: ObjectId, packVer: String?) {
        val packVer = packVer ?: "latest"
        val team = TeamService.getOwn(uid) ?: throw RequestError("你不是队长")
        if (!team.hasHost(hostId)) throw RequestError("此主机不属于你的团队")
        val host = getById(hostId) ?: throw RequestError("无此主机")
        val running = DockerService.isStarted(hostId.str)
        if (running) {
            DockerService.stop(hostId.str)
        }
        DockerService.deleteContainer(hostId.str)
        val worldId = host.worldId
        //TODO 暂时不支持自定义整合包 只能用官方的
        DockerService.createContainer(host.port, hostId.str, worldId.str, "${host.modpackId.str}:$packVer")

        DockerService.start(hostId.str)
        clearShutFlag(hostId)

        dbcl.updateOne(
            eq("_id", host._id), combine(
                set(Host::packVer.name, packVer),
            )
        )
    }

    suspend fun start(uid: ObjectId, hostId: ObjectId) {
        val host = getById(hostId) ?: throw RequestError("无此主机")
        val team = TeamService.get(host.teamId) ?: throw RequestError("无此团队")
        if (!team.hasHost(hostId)) throw RequestError("此主机不属于你的团队")
        if (!team.isOwnerOrAdmin(uid)) throw RequestError("无权限")
        DockerService.start(hostId.str)
        clearShutFlag(hostId)
    }

    suspend fun stop(uid: ObjectId, hostId: ObjectId) {
        val host = getById(hostId) ?: throw RequestError("无此主机")
        val team = TeamService.get(host.teamId) ?: throw RequestError("无此团队")
        if (!team.hasHost(hostId)) throw RequestError("此主机不属于你的团队")
        if (!team.isOwnerOrAdmin(uid)) throw RequestError("无权限")
        sendCommand(uid,hostId,"stop")
        //DockerService.stop(hostId.str)
        clearShutFlag(hostId)
    }

    suspend fun restart(uid: ObjectId, hostId: ObjectId) {
        val host = getById(hostId) ?: throw RequestError("无此主机")
        val team = TeamService.get(host.teamId) ?: throw RequestError("无此团队")
        if (!team.hasHost(hostId)) throw RequestError("此主机不属于你的团队")
        if (!team.isOwnerOrAdmin(uid)) throw RequestError("无权限")
        DockerService.restart(hostId.str)
        clearShutFlag(hostId)
    }

    suspend fun sendCommand(uid: ObjectId, hostId: ObjectId, command: String) {
        val host = getById(hostId) ?: throw RequestError("无此主机")
        val team = TeamService.get(host.teamId) ?: throw RequestError("无此团队")
        if (!team.hasHost(hostId)) throw RequestError("此主机不属于你的团队")
        if (!team.isOwnerOrAdmin(uid)) throw RequestError("无权限")

        val normalized = command.trimEnd()
        if (normalized.isBlank()) throw RequestError("命令不能为空")

        val session = hostStates[hostId]?.session ?: throw RequestError("主机未处于游玩状态")

        val message = WsMessage(
            channel = WsMessage.Channel.command,
            data = normalized
        )

        val payload = serdesJson.encodeToString(
            message
        )

        try {
            session.send(Frame.Text(payload))
        } catch (cancel: CancellationException) {
            hostStates[hostId]?.let { state ->
                if (state.session === session) {
                    state.session = null
                    if (state.shutFlag <= 0) {
                        hostStates.remove(hostId, state)
                    }
                }
            }
            throw cancel
        } catch (t: Throwable) {
            hostStates[hostId]?.let { state ->
                if (state.session === session) {
                    state.session = null
                    if (state.shutFlag <= 0) {
                        hostStates.remove(hostId, state)
                    }
                }
            }
            lgr.warn(t) { "发送命令到 $hostId 失败: ${t.message}" }
            throw RequestError("发送命令失败: ${t.message ?: "未知错误"}")
        }
    }

    suspend fun getServerStatus(hostId: ObjectId): HostStatus {
        val host = getById(hostId) ?: return HostStatus.UNKNOWN
        val status = DockerService.getContainerStatus(host._id.str)
        if (status == HostStatus.STARTED && hostStates[host._id]?.session != null) {
            return HostStatus.PLAYABLE
        }
        return status
    }

    // Get logs by range: [startLine, endLine), 0 means newest line
    suspend fun getLog(uid: ObjectId, hostId: ObjectId, startLine: Int = 0, needLines: Int): String {
        if (needLines > 200) throw RequestError("行数太多")
        val host = getById(hostId) ?: throw RequestError("无此主机")
        val team = TeamService.get(host.teamId) ?: throw RequestError("无此团队")
        if (!team.hasHost(hostId)) throw RequestError("此主机不属于你的团队")
        if (!team.isOwnerOrAdmin(uid)) throw RequestError("无权限")
        return DockerService.getLog(hostId.str, startLine, startLine + needLines)
    }

    // ---------- Streaming Helper (still needs ApplicationCall for SSE) ----------
    suspend fun listenLogs(uid: ObjectId, hostId: ObjectId, session: ServerSSESession) {
        val host = getById(hostId) ?: throw RequestError("无此主机")
        val team = TeamService.get(host.teamId) ?: throw RequestError("无此团队")
        if (!team.hasHost(hostId)) throw RequestError("此主机不属于你的团队")
        if (!team.isOwnerOrAdmin(uid)) throw RequestError("无权限")

        val containerName = hostId.str
        val lines = Channel<String>(capacity = Channel.BUFFERED)
        val subscription = DockerService.listenLog(
            containerName,
            onLine = { lines.trySend(it).isSuccess },
            onError = { lines.close(it) },
            onFinished = { lines.close() }
        )

        session.heartbeat {
            period = 15.seconds
            event = ServerSentEvent(event = "heartbeat", data = "ping")
        }

        try {
            for (payload in lines) {
                payload.lineSequence()
                    .map { it.trimEnd('\r') }
                    .filter { it.isNotEmpty() }
                    .forEach { session.send(ServerSentEvent(data = it)) }
            }
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (t: Throwable) {
            try {
                session.send(ServerSentEvent(event = "error", data = t.message ?: "unknown"))
            } catch (_: Throwable) {
            }
        } finally {
            runCatching { subscription.close() }
            lines.cancel()
        }
    }

    // List all hosts belonging to a team
    suspend fun listByTeam(teamId: ObjectId): List<Host> =
        dbcl.find(eq("teamId", teamId)).toList()


    suspend fun findByWorld(worldId: ObjectId): Host? =
        dbcl.find(eq("worldId", worldId)).firstOrNull()

    suspend fun getById(id: ObjectId): Host? = dbcl.find(eq("_id", id)).firstOrNull()

    suspend fun remountWorld(host: Host, newWorldId: ObjectId?) {
        val containerName = host._id.str
        val wasRunning = DockerService.isStarted(containerName)
        if (wasRunning) {
            try {
                DockerService.stop(containerName)
            } catch (_: Throwable) {
            }
        }
        DockerService.deleteContainer(containerName)
        DockerService.createContainer(host.port, containerName, newWorldId?.str, host.imageRef())
        dbcl.updateOne(eq("_id", host._id), set(Host::worldId.name, newWorldId))
        if (newWorldId != null && wasRunning) {
            DockerService.start(containerName)
        }
        clearShutFlag(host._id)
    }

    suspend fun stopIdleHosts() {
        runIdleMonitorTick(forceStop = true)
    }
}

