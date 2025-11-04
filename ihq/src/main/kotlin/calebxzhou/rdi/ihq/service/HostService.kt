package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.DB
import calebxzhou.rdi.ihq.DEFAULT_MODPACK_ID
import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.model.Host
import calebxzhou.rdi.ihq.model.Team
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
import calebxzhou.rdi.ihq.model.pack.Mod
import calebxzhou.rdi.ihq.service.HostService.addExtraMods
import calebxzhou.rdi.ihq.service.HostService.delExtraMods
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import io.ktor.server.request.receive
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
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.minutes
import org.bson.types.ObjectId

// ---------- Routing DSL (mirrors teamRoutes style) ----------
fun Route.hostRoutes() = route("/host") {
    route("/{hostId}/status") {
        install(HostGuardPlugin) { permission = HostPermission.TEAM_MEMBER }
        get {
            val ctx = call.hostGuardContext()
            response(data = HostService.getServerStatus(ctx.host).toString())
        }
    }

    route("/{hostId}/start") {
        install(HostGuardPlugin) { permission = HostPermission.ADMIN_OR_OWNER }
        post {
            HostService.start(call.hostGuardContext())
            ok()
        }
    }

    route("/{hostId}/stop") {
        install(HostGuardPlugin) { permission = HostPermission.ADMIN_OR_OWNER }
        post {
            HostService.userStop(call.hostGuardContext())
            ok()
        }
    }

    route("/{hostId}/command") {
        install(HostGuardPlugin) { permission = HostPermission.ADMIN_OR_OWNER }
        post {
            val ctx = call.hostGuardContext()
            val command = param("command")
            HostService.sendCommand(ctx, command)
            ok()
        }
    }

    route("/{hostId}/restart") {
        install(HostGuardPlugin) { permission = HostPermission.ADMIN_OR_OWNER }
        post {
            HostService.restart(call.hostGuardContext())
            ok()
        }
    }

    route("/{hostId}/update") {
        install(HostGuardPlugin) { permission = HostPermission.OWNER_ONLY }
        post {
            val ctx = call.hostGuardContext()
            HostService.update(ctx, paramNull("packVer"))
            ok()
        }
    }

    route("/") {
        install(TeamGuardPlugin) { permission = TeamPermission.OWNER_ONLY }
        post {
            val ctx = call.teamGuardContext()
            HostService.create(
                ctx.team,
                ctx.requester.id,
                DEFAULT_MODPACK_ID,
                "latest",
                paramNull("worldId")?.let { ObjectId(it) }
            )
            ok()
        }
    }

    route("/{hostId}") {
        install(HostGuardPlugin) { permission = HostPermission.OWNER_ONLY }
        delete {
            HostService.delete(call.hostGuardContext())
            ok()
        }
    }

    route("/{hostId}/log/{lines}") {
        install(HostGuardPlugin) { permission = HostPermission.ADMIN_OR_OWNER }
        get {
            val ctx = call.hostGuardContext()
            val lines = param("lines").toInt()
            val startLine = paramNull("startLine")?.toInt() ?: 0
            response(data = HostService.getLog(ctx, startLine, lines))
        }
    }

    route("/{hostId}/log/stream") {
        install(HostGuardPlugin) { permission = HostPermission.ADMIN_OR_OWNER }
        sse {
            HostService.listenLogs(call.hostGuardContext(), this)
        }
    }

    route("/{hostId}/extra_mod") {
        install(HostGuardPlugin)

        get {
            response(data = call.hostGuardContext().host.mods)
        }
        post {
            val ctx = call.hostGuardContext()
            ctx.requirePermission(HostPermission.ADMIN_OR_OWNER)
            val mods = call.receive<List<Mod>>()
            ctx.addExtraMods(mods)
        }
        delete {
            val ctx = call.hostGuardContext()
            ctx.requirePermission(HostPermission.ADMIN_OR_OWNER)
            val projectIds = call.receive<List<String>>()
            ctx.delExtraMods(projectIds)
        }
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

    suspend fun getPlayables(): List<Host> = withContext(Dispatchers.IO) {
        val runningIds = DockerService.listContainers(includeStopped = false)
            .mapNotNull { container ->
                val containerName = container.names?.firstOrNull()?.removePrefix("/") ?: return@mapNotNull null
                runCatching { ObjectId(containerName) }.getOrNull()
            }
            .distinct()

        if (runningIds.isEmpty()) {
            emptyList()
        } else {
            dbcl.find(`in`("_id", runningIds)).toList()
                .filter { host -> hostStates[host._id]?.session != null }
        }
    }

    suspend fun getIdles(): List<Host> {
        val result = mutableListOf<Host>()
        for (host in getPlayables()) {
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
            getPlayables()
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

    suspend fun create(team: Team, requesterId: ObjectId, modpackId: ObjectId, packVer: String, worldId: ObjectId?) {
        PlayerService.getById(requesterId) ?: throw RequestError("无此账号")
        val world = worldId?.let {
            if (findByWorld(it) != null) throw RequestError("此存档已被其他主机占用")
            WorldService.getById(it) ?: throw RequestError("无此存档")
        } ?: WorldService.create(requesterId, null, modpackId)
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

    suspend fun delete(ctx: HostGuardContext) {
        delete(ctx.team, ctx.host._id)
    }

    suspend fun delete(team: Team, hostId: ObjectId) {
        val host = getById(hostId) ?: return
        delete(team, host)
    }

    suspend fun delete(team: Team, host: Host) {
        if (host.teamId != team._id) throw RequestError("主机不属于你的团队")
        dbcl.deleteOne(eq("_id", host._id))
        team.delHost(host._id)
        DockerService.deleteContainer(host._id.str)
        clearShutFlag(host._id)
    }

    suspend fun update(ctx: HostGuardContext, packVer: String?) {
        val host = getById(ctx.host._id) ?: throw RequestError("无此主机")
        val resolvedVer = packVer ?: "latest"
        val running = DockerService.isStarted(host._id.str)
        if (running) {
            DockerService.stop(host._id.str)
        }
        DockerService.deleteContainer(host._id.str)
        val worldId = host.worldId
        DockerService.createContainer(host.port, host._id.str, worldId.str, "${host.modpackId.str}:$resolvedVer")
        DockerService.start(host._id.str)
        clearShutFlag(host._id)

        dbcl.updateOne(
            eq("_id", host._id), combine(
                set(Host::packVer.name, resolvedVer),
            )
        )
    }

    suspend fun start(ctx: HostGuardContext) {
        val host = getById(ctx.host._id) ?: throw RequestError("无此主机")
        DockerService.start(host._id.str)
        clearShutFlag(host._id)
    }

    suspend fun userStop(ctx: HostGuardContext) {
        sendCommand(ctx, "stop")
        clearShutFlag(ctx.host._id)
    }

    suspend fun restart(ctx: HostGuardContext) {
        val host = getById(ctx.host._id) ?: throw RequestError("无此主机")
        DockerService.restart(host._id.str)
        clearShutFlag(host._id)
    }

    suspend fun sendCommand(ctx: HostGuardContext, command: String) {
        val hostId = ctx.host._id
        val normalized = command.trimEnd()
        if (normalized.isBlank()) throw RequestError("命令不能为空")

        val session = hostStates[hostId]?.session ?: throw RequestError("主机未处于游玩状态")

        val message = WsMessage(
            channel = WsMessage.Channel.command,
            data = normalized
        )

        val payload = serdesJson.encodeToString(message)

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

    suspend fun getServerStatus(host: Host): HostStatus {
        val status = DockerService.getContainerStatus(host._id.str)
        if (status == HostStatus.STARTED && hostStates[host._id]?.session != null) {
            return HostStatus.PLAYABLE
        }
        return status
    }

    suspend fun getServerStatus(hostId: ObjectId): HostStatus {
        val host = getById(hostId) ?: return HostStatus.UNKNOWN
        return getServerStatus(host)
    }

    // Get logs by range: [startLine, endLine), 0 means newest line
    suspend fun getLog(ctx: HostGuardContext, startLine: Int = 0, needLines: Int): String {
        if (needLines > 200) throw RequestError("行数太多")
        val hostId = ctx.host._id
        return DockerService.getLog(hostId.str, startLine, startLine + needLines)
    }

    // ---------- Streaming Helper (still needs ApplicationCall for SSE) ----------
    suspend fun listenLogs(ctx: HostGuardContext, session: ServerSSESession) {
        val hostId = ctx.host._id
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
    suspend fun HostGuardContext.addExtraMods(mods: List<Mod>) {
        if (mods.isEmpty()) throw RequestError("mod列表不得为空")

        val currentHost = getById(host._id) ?: throw RequestError("无此主机")
        val merged = currentHost.mods.toMutableList()

        mods.forEach { mod ->
            val existingIndex = merged.indexOfFirst { extra ->
                extra.projectId == mod.projectId
            }
            if (existingIndex >= 0) {
                val existing = merged[existingIndex]
                if (existing != mod) {
                    merged[existingIndex] = mod
                }
            } else {
                merged += mod
            }
        }

        dbcl.updateOne(
            eq("_id", currentHost._id),
            set(Host::mods.name, merged.toList())
        )
    }
    suspend fun HostGuardContext.delExtraMods(projectIds: List<String>) {
        if (projectIds.isEmpty()) throw RequestError("mod列表不得为空")

        val currentHost = getById(host._id) ?: throw RequestError("无此主机")
        val targets = projectIds.toSet()

        val updated = currentHost.mods.filterNot { it.projectId in targets }

        if (updated.size == currentHost.mods.size) throw RequestError("没有匹配的mod可删")

        dbcl.updateOne(
            eq("_id", currentHost._id),
            set(Host::mods.name, updated)
        )
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

