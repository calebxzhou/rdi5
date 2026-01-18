package calebxzhou.rdi.master.service

import calebxzhou.mykotutils.log.Loggers
import calebxzhou.mykotutils.std.deleteRecursivelyNoSymlink
import calebxzhou.mykotutils.std.displayLength
import calebxzhou.mykotutils.std.jarResource
import calebxzhou.mykotutils.std.readAllString
import calebxzhou.rdi.common.json
import calebxzhou.rdi.common.model.*
import calebxzhou.rdi.common.serdesJson
import calebxzhou.rdi.common.util.ioScope
import calebxzhou.rdi.common.util.objectId
import calebxzhou.rdi.common.util.str
import calebxzhou.rdi.master.DB
import calebxzhou.rdi.master.HOSTS_DIR
import calebxzhou.rdi.master.exception.ParamError
import calebxzhou.rdi.master.exception.RequestError
import calebxzhou.rdi.master.model.WsMessage
import calebxzhou.rdi.master.net.*
import calebxzhou.rdi.master.service.HostService.addMember
import calebxzhou.rdi.master.service.HostService.changeGameRules
import calebxzhou.rdi.master.service.HostService.changeOptions
import calebxzhou.rdi.master.service.HostService.changeVersion
import calebxzhou.rdi.master.service.HostService.createHost
import calebxzhou.rdi.master.service.HostService.createHostLegacy
import calebxzhou.rdi.master.service.HostService.delMember
import calebxzhou.rdi.master.service.HostService.delete
import calebxzhou.rdi.master.service.HostService.forceStop
import calebxzhou.rdi.master.service.HostService.getLog
import calebxzhou.rdi.master.service.HostService.graceStop
import calebxzhou.rdi.master.service.HostService.hostContext
import calebxzhou.rdi.master.service.HostService.listHostLobby
import calebxzhou.rdi.master.service.HostService.listenLogs
import calebxzhou.rdi.master.service.HostService.needAdmin
import calebxzhou.rdi.master.service.HostService.needOwner
import calebxzhou.rdi.master.service.HostService.restart
import calebxzhou.rdi.master.service.HostService.sendCommand
import calebxzhou.rdi.master.service.HostService.setRole
import calebxzhou.rdi.master.service.HostService.start
import calebxzhou.rdi.master.service.HostService.status
import calebxzhou.rdi.master.service.HostService.transferOwnership
import calebxzhou.rdi.master.service.ModpackService.getVersion
import calebxzhou.rdi.master.service.ModpackService.installToHost
import calebxzhou.rdi.master.service.WorldService.createWorld
import calebxzhou.rdi.model.Role
import com.github.dockerjava.api.model.Mount
import com.github.dockerjava.api.model.MountType
import com.github.dockerjava.api.model.TmpfsOptions
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.server.websocket.*
import io.ktor.sse.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.bson.Document
import org.bson.conversions.Bson
import org.bson.types.ObjectId
import java.io.File
import java.util.Properties
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

val Host.dir get() = HOSTS_DIR.resolve(_id.str)

// ---------- Routing DSL (mirrors teamRoutes style) ----------
fun Route.hostRoutes() = route("/host") {
    route("") {
        //旧版接口 保持兼容
        post {
            call.player().createHostLegacy(
                idParam("modpackId"),
                param("packVer"),
                param("worldOpr"),
                paramNull("useWorld")?.let { ObjectId(it) },
                param("difficulty").toInt(),
                param("gameMode").toInt(),
                param("levelType"),
                false,
                param("gameRules").let { serdesJson.decodeFromString<MutableMap<String, String>>(it) }
            )
            ok()
        }
        post("/v2") {
            call.player().createHost(call.receive())
            ok()
        }

        get("/lobby/{page?}") {
            val hosts = call.player().listHostLobby(paramNull("page")?.toInt() ?: 0)
            response(data = hosts)
        }
        get("/search/modpack/{modpackId}/{verName}") {
            val hosts = HostService.findByModpackVersion(idParam("modpackId"), param("verName"))
            response(data = hosts)
        }
        get("/my/{page?}") {
            response(data = call.player().listHostLobby(paramNull("page")?.toInt() ?: 0, myOnly = true))
        }
    }
    route("/{hostId}") {
        get("/status") {
            call.hostContext().host.status.let { response(data = it) }
        }
        post("/start") {
            call.hostContext().start()
            ok()
        }
        post("/stop") {
            call.hostContext().needAdmin.graceStop()
            ok()
        }
        post("/force-stop") {
            call.hostContext().needAdmin.forceStop()
            ok()
        }
        post("/command") {
            val ctx = call.hostContext().needAdmin
            ctx.sendCommand(param("command"))
            ok()
        }
        post("/restart") {
            call.hostContext().needAdmin.restart()
            ok()
        }
        put("/options") {
            val ctx = call.hostContext().needAdmin
            ctx.changeOptions(call.receive<Host.OptionsDto>())
            ok()
        }
        /*put("/gamerules") {
            call.hostContext().needAdmin.changeGameRules(call.paramT("data"))
            ok()
        }*/
        post("/update") {
            call.hostContext().needAdmin.changeVersion(paramNull("verName"))
            ok()
        }
        post("/transfer/{uid2}") {
            call.hostContext().needOwner.transferOwnership()
            ok()
        }
        delete {
            call.hostContext().needOwner.delete()
            ok()
        }
        get {
            HostService.getById(idParam("hostId"))?.let {
                response(data = it)
            } ?: err("无此地图")
        }
        /*post("/modpack/{modpackId}/{verName}") {
            call.hostContext().needAdmin.changeModpack(idParam("modpackId"), param("verName"))
            ok()
        }*/

        route("/log") {
            sse("/stream") {
                val ctx = try {
                    call.hostContext()
                } catch (err: RequestError) {
                    runCatching {
                        send(ServerSentEvent(event = "error", data = err.message ?: "unknown"))
                    }
                    return@sse
                }
                ctx.listenLogs(this)

            }
            get("/{lines}") {
                val logs = call.hostContext().getLog(paramNull("startLine")?.toInt() ?: 0, param("lines").toInt())
                response(data = logs)
            }
        }
        route("/member/{uid2}") {
            put("/role/{role}") {
                call.hostContext().needOwner.setRole(Role.valueOf(param("role")))
                ok()
            }
            delete {
                call.hostContext().needOwner.delMember()
                ok()
            }
        }
        post("/member/{qq}") {
            call.hostContext().needAdmin.addMember(param("qq"))
            ok()

        }
    }


}

//单独拿出来是为了不走authentication
fun Route.hostPlayRoutes() = route("/host") {
    get("/status") {
        val port = param("port").toInt()
        val host = HostService.getByPort(port) ?: throw RequestError("无此地图")
        response(data = host.status)
    }
    webSocket("/play/{hostId}") {
        val rawHostId = call.param("hostId")

        val hostId = runCatching { ObjectId(rawHostId) }.getOrElse {
            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "host无效"))
            return@webSocket
        }

        val host = HostService.getById(hostId)
        if (host == null) {
            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "未知地图"))
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

data class HostContext(
    val host: Host,
    val player: RAccount,
    val member: Host.Member,
    val targetMemberNull: Host.Member?
) {
    var reqId = 0
    val targetMember get() = targetMemberNull ?: throw ParamError("玩家${player.name}不是此地图的受邀成员")
}

object HostService {
    private val lgr by Loggers
    private val isWindowsHost: Boolean = System.getProperty("os.name").contains("windows", ignoreCase = true)
    private val dockerDesktopPrefix: String =
        System.getenv("DOCKER_DESKTOP_PATH_PREFIX")?.trimEnd('/') ?: "/run/desktop/mnt/host"

    val dbcl = DB.getCollection<Host>("host")

    init {
        runBlocking {
            dbcl.createIndex(Indexes.ascending("port"))
        }
    }

    private const val PORT_START = 50000
    const val SERVER_RDI_CORE_FILENAME = "rdi-5-server.jar"
    private const val PORT_END_EXCLUSIVE = 60000
    private const val SHUTDOWN_THRESHOLD = 10
    private const val HOSTS_PER_PAGE = 20

    private val idleMonitorScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val modDownloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var idleMonitorJob: Job? = null

    private data class HostState(
        var shutFlag: Int = 0,
        var session: DefaultWebSocketServerSession? = null
    )

    private val hostStates = ConcurrentHashMap<ObjectId, HostState>()


    private val Host.containerEnv
        get() = { mcVer: McVersion, mods: List<Mod>, gameRules: Map<String, String> ->
            mutableListOf(
                "HOST_ID=${_id.str}",
                "GAME_PORT=${port}",
                "MOD_LIST=${mods.joinToString("\n") { mod -> mod.fileName }}",
                "ALL_OP=${if (allowCheats) "true" else "false"}",
                "START_PARAMS=-Xmx8G ${
                    when (mcVer) {
                        //以后会支持其他mod loader 暂时只支持forge/neoforge
                        McVersion.V211 -> "@libraries/net/neoforged/neoforge/21.1.217/unix_args.txt"
                        McVersion.V201 -> "@libraries/net/minecraftforge/forge/1.20.1-47.4.13/unix_args.txt"
                    }
                } --universe /data --nogui"
            ).apply {
                gameRules.forEach { id, value ->
                    this += "GAME_RULE_${id}=${value}"
                }
            }
        }


    private data class OverlaySources(
        val libsDir: File,
        val versionDir: File
    )

    private fun Host.prepareOverlaySources(modpack: Modpack, version: Modpack.Version): OverlaySources {
        val libsDir = modpack.libsDir.canonicalFile.also {
            if (!it.exists()) throw RequestError("整合包依赖缺失: ${it}")
        }
        val versionDir = version.dir.canonicalFile.also {
            if (!it.exists()) throw RequestError("整合包版本目录缺失: ${it}")
        }
        val hostRoot = overlayRootDir().apply { mkdirs() }


        return OverlaySources(libsDir, versionDir)
    }

    val HostContext.needAdmin get() = requireRole(Role.ADMIN)
    val HostContext.needOwner get() = requireRole(Role.OWNER)
    fun HostContext.requireRole(level: Role): HostContext {
        member.let {
            val allowed = when (level) {
                Role.MEMBER -> true
                Role.ADMIN -> member.role.level <= Role.ADMIN.level
                Role.OWNER -> member.role == Role.OWNER
                else -> false
            }
            if (!allowed) throw RequestError("无权限")
        }
        return this
    }


    suspend fun ApplicationCall.hostContext(): HostContext {
        val requesterId = uid
        val host = HostService.getById(idParam("hostId")) ?: throw RequestError("无此地图")
        val reqMem = host.members.firstOrNull { it.id == requesterId } ?: run {
            if (PlayerService.getName(host.ownerId) == "davickk") {
                Host.Member(id = requesterId, role = Role.MEMBER)
            } else {
                throw RequestError("不是地图受邀成员")
            }
        }
        val tarMem = idParamNull("uid2")?.let { uid2 -> host.members.find { it.id == uid2 } }
        return HostContext(host, player(), reqMem, tarMem)
    }

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
            if (host.getOnlinePlayers().size == 0) {
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

            if (onlinePlayers.isEmpty()) {
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
            lgr.info { "upd shut flag $hostId $value" }
        }
    }

    private fun clearShutFlag(hostId: ObjectId) {
        hostStates[hostId]?.let { state ->
            state.shutFlag = 0
            if (state.session == null) {
                hostStates.remove(hostId, state)
            }
        }
        lgr.info { "保持在线 $hostId" }
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
    suspend fun Host.getOnlinePlayers(): List<ObjectId> {
        return try {
            if(status!= HostStatus.PLAYABLE)
                return emptyList()
            McServerPinger.ping(this.port).players?.let { players ->
                players.sample.map { UUID.fromString(it.id).objectId }
            } ?: emptyList()
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (t: Throwable) {
            lgr.warn(t) { "Failed to ping host ${this._id}: ${t.message}" }
            emptyList()
        }
    }

    suspend fun RAccount.createHostLegacy(
        modpackId: ObjectId,
        packVer: String,
        worldOpr: String,
        worldId: ObjectId?,
        difficulty: Int,
        gameMode: Int,
        levelType: String,
        allowCheats: Boolean,
        gameRules: MutableMap<String, String>
    ) {
        val playerId = _id
        if (getByOwner(playerId).size > 3) {
            throw RequestError("每玩家最多只可创建3张地图")
        }
        val world = worldId?.let {
            val occupyHost = findByWorld(it)
            if (occupyHost != null)
                throw RequestError("此存档数据已被地图“${occupyHost.name}”占用")
            WorldService.getById(it)?.also { world ->
                if (world.ownerId != playerId) throw RequestError("不是你的存档")
            } ?: throw RequestError("无此存档")
        } ?: let {
            when (worldOpr) {
                "create" -> createWorld(playerId, null, modpackId)
                "use" -> throw ParamError("缺少存档ID")
                "no" -> null
                else -> null
            }
        }

        val modpack = ModpackService.getById(modpackId) ?: throw RequestError("无此包")
        val version = modpack.getVersion(packVer) ?: throw RequestError("无此版本")
        val port = allocateRoomPort()
        val host = Host(
            name = "${name}的世界-" + (ownHosts().size + 1),
            ownerId = playerId,
            modpackId = modpackId,
            packVer = packVer,
            worldId = world?._id,
            port = port,
            difficulty = difficulty,
            gameMode = gameMode,
            levelType = levelType,
            members = listOf(Host.Member(id = playerId, role = Role.OWNER)),
            gameRules = gameRules
        )
        val mailId =
            MailService.sendSystemMail(playerId, "地图创建中", "${host.name}正在创建中，请稍等几分钟...")._id
        dbcl.insertOne(host)
        startCreateHost(host, modpack, version, mailId)


    }

    suspend fun RAccount.createHost(host: Host.CreateDto) {
        if (host.name.displayLength !in 3..24) {
            throw RequestError("名称长度应在3~24，当前为${host.name.displayLength}（一个汉字算两个长度）")
        }
        val playerId = _id
        if (getByOwner(playerId).size > 3) {
            throw RequestError("最多只可创建3张地图")
        }
        val world = host.worldId?.let {
            val occupyHost = findByWorld(it)
            if (occupyHost != null)
                throw RequestError("此存档数据已被地图“${occupyHost.name}”占用")
            WorldService.getById(it)?.also { world ->
                if (world.ownerId != playerId) throw RequestError("不是你的存档")
            } ?: throw RequestError("无此存档")
        } ?: let {
            if (host.saveWorld) {
                createWorld(playerId, null, host.modpackId)
            } else {
                null
            }
        }
        val modpack = ModpackService.getById(host.modpackId) ?: throw RequestError("无此包")
        val version = modpack.getVersion(host.packVer) ?: throw RequestError("无此版本")
        val port = allocateRoomPort()
        val host = Host(
            name = "${name}的世界-" + (ownHosts().size + 1),
            ownerId = playerId,
            modpackId = host.modpackId,
            packVer = host.packVer,
            worldId = world?._id,
            port = port,
            difficulty = host.difficulty,
            allowCheats = host.allowCheats,
            gameMode = host.gameMode,
            levelType = host.levelType,
            members = listOf(Host.Member(id = playerId, role = Role.OWNER)),
            gameRules = host.gameRules
        )
        val mailId =
            MailService.sendSystemMail(playerId, "地图创建中", "${host.name}正在创建中，请稍等几分钟...")._id
        dbcl.insertOne(host)
        startCreateHost(host, modpack, version, mailId)


    }

    private fun startCreateHost(
        host: Host,
        modpack: Modpack,
        version: Modpack.Version,
        mailId: ObjectId
    ) {
        ioScope.launch {
            runCatching {
                if (host.dir.exists()) {
                    host.dir.deleteRecursivelyNoSymlink()
                }
                host.dir.mkdir()

                host.makeContainer(host.worldId, modpack, version, host.gameRules)

                modpack.installToHost(host.packVer, host) {
                    MailService.changeMail(mailId, "地图创建中", newContent = it)
                }
                "server.properties".run {
                    this.jarResource(this).readAllString()
                        .replace("#{port}", host.port.toString())
                        .replace(
                            "#{difficulty}", when (host.difficulty) {
                                0 -> "peaceful"
                                1 -> "easy"
                                2 -> "normal"
                                3 -> "hard"
                                else -> "normal"
                            }
                        )
                        .replace("#{level-type}", host.levelType)
                        .replace(
                            "#{gamemode}", when (host.gameMode) {
                                0 -> "survival"
                                1 -> "creative"
                                2 -> "adventure"
                                else -> "survival"
                            }
                        ).let {
                            host.dir.resolve(this).writeText(it)
                        }
                }
                val defaultPropsFile = host.dir.resolve("default-server.properties")
                val serverPropsFile = host.dir.resolve("server.properties")
                if (defaultPropsFile.exists() && serverPropsFile.exists()) {
                    val serverProps = Properties().apply {
                        serverPropsFile.inputStream().use { load(it) }
                    }
                    val defaultProps = Properties().apply {
                        defaultPropsFile.inputStream().use { load(it) }
                    }
                    defaultProps.forEach { key, value ->
                        if (key.toString() != "server-port") {
                            lgr.info { "apply prop $key = $value" }
                            serverProps.setProperty(key.toString(), value.toString())
                        }
                    }
                    serverPropsFile.outputStream().use { serverProps.store(it, null) }
                }
                lgr.info { "installToHost returned. Proceeding to start Docker container for host ${host._id} (Logic Error Tracing)." }
                DockerService.start(host._id.str)

                clearShutFlag(host._id)
            }.onFailure {
                lgr.error { it }
                it.printStackTrace()
                MailService.changeMail(mailId, "地图创建失败", newContent = "无法创建地图，错误：${it}")
            }.onSuccess {
                MailService.changeMail(mailId, "地图创建成功", newContent = "可以玩了")
            }
        }
    }

    /*suspend fun HostContext.changeModpack(modpackId: ObjectId, verName: String) {
        if (host.status != HostStatus.STOPPED)
            throw RequestError("请先停止主机")
        val modpack = ModpackService.getById(modpackId) ?: throw RequestError("无此整合包")
        val version = modpack.getVersion(verName) ?: throw RequestError("无此版本")
        DockerService.deleteContainer(host._id.str)
        host.makeContainer(host.worldId, modpack, version)
        dbcl.updateOne(
            eq("_id", host._id), combine(
                set(Host::modpackId.name, modpackId),
                set(Host::packVer.name, verName),
            )
        )
    }*/

    private fun Host.makeContainer(
        worldId: ObjectId?,
        modpack: Modpack,
        version: Modpack.Version,
        gameRules: Map<String, String>
    ) {

        val mounts = mutableListOf(
            Mount()
                .withType(MountType.BIND)
                .withSource(dir.absolutePath)
                .withTarget("/opt/server"),
        ).apply {

            if (worldId != null) {
                //使用存档
                this += Mount()
                    .withType(MountType.BIND)
                    .withSource(WorldService.getDataDir(worldId).absolutePath)
                    .withTarget("/data")
            } else {
                //不存档
                this += Mount()
                    .withType(MountType.TMPFS)
                    .withTarget("/data")
                    .withTmpfsOptions(TmpfsOptions().withSizeBytes(512 * 1024 * 1024))
            }
        }
        val image = when (modpack.mcVer) {
            McVersion.V211, McVersion.V201 -> "rdi:j21"
            // V165 V122 V071 -> "rdi:j8"
            else -> throw RequestError("不支持的MC版本")
        }
        DockerService.createContainer(
            port,
            this._id.str,
            mounts,
            image,
            containerEnv(modpack.mcVer, version.mods, gameRules)
        )
    }

    suspend fun HostContext.delete() {
        if (host.status == HostStatus.PLAYABLE) {
            graceStop()
        }
        host.dir.deleteRecursivelyNoSymlink()
        host.dir.delete()
        DockerService.deleteContainer(host._id.str)
        clearShutFlag(host._id)
        dbcl.deleteOne(eq("_id", host._id))
    }

    suspend fun HostContext.changeVersion(packVer: String?) {
        /*if (host.status != HostStatus.STOPPED) {
            throw RequestError("请先停止主机")
        }*/
        val modpack = ModpackService.getById(host.modpackId) ?: throw RequestError("无此整合包")
        val modpackVer = modpack.versions.find { it.name == packVer } ?: modpack.versions.lastOrNull()
        ?: throw RequestError("无可用版本")
        val resolvedVer = modpackVer.name
        val hostIdStr = host._id.str
        val mailId = MailService.sendSystemMail(
            player._id,
            "地图整合包切换中",
            "你的地图《${host.name}》正在切换到整合包版本 $resolvedVer ，请稍等几分钟..."
        )._id

        DockerService.deleteContainer(hostIdStr)
        clearShutFlag(host._id)
        dbcl.updateOne(
            eq("_id", host._id), combine(
                set(Host::packVer.name, resolvedVer),
            )
        )
        host.packVer = resolvedVer
        startCreateHost(host, modpack, modpackVer, mailId)
        MailService.changeMail(mailId, "地图整合包版本切换完成", "好了")
    }

    suspend fun HostContext.changeGameRules(newRules: Map<String, String>) {
        /*if (host.status != HostStatus.STOPPED) {
            throw RequestError("请先停止主机")
        }*/
        val modpack = ModpackService.getById(host.modpackId) ?: throw RequestError("无此整合包")
        val version = modpack.getVersion(host.packVer) ?: throw RequestError("无此版本")

        // Recreate container with updated environment
        DockerService.deleteContainer(host._id.str)
        host.gameRules.clear()
        host.gameRules.putAll(newRules)
        dbcl.updateOne(eq("_id", host._id), set(Host::gameRules.name, newRules))
        host.makeContainer(host.worldId, modpack, version, newRules)
        clearShutFlag(host._id)
    }

    suspend fun HostContext.changeOptions(payload: Host.OptionsDto) {
        payload.gameRules?.let { rules ->
            changeGameRules(rules)
        }
        val updates = mutableListOf<Bson>()
        payload.difficulty?.let { updates += set(Host::difficulty.name, it) }
        payload.gameMode?.let { updates += set(Host::gameMode.name, it) }
        payload.levelType?.takeIf { it.isNotBlank() }?.let { updates += set(Host::levelType.name, it) }
        payload.whitelist?.let { updates += set(Host::whitelist.name, it) }
        payload.allowCheats?.let { updates += set(Host::allowCheats.name, it) }
        if (updates.isNotEmpty()) {
            val update = if (updates.size == 1) updates.first() else combine(updates)
            dbcl.updateOne(eq("_id", host._id), update)
        }
    }

    suspend fun HostContext.start() {
        val current = getById(host._id) ?: throw RequestError("无此地图")
        if (DockerService.isStarted(current._id.str)) {
            throw RequestError("已经启动过了")
        }
        if (DockerService.findContainer(current._id.str) == null) {
            throw RequestError("地图版本过旧 请点击地图详细信息界面的“更新”按钮")
            return
        }
        DockerService.start(current._id.str)
        clearShutFlag(current._id)
    }

    suspend fun HostContext.graceStop() {
        sendCommand("stop")
        clearShutFlag(host._id)
    }

    suspend fun HostContext.forceStop() {
        clearShutFlag(host._id)
        DockerService.stop(host._id.str)
    }

    suspend fun HostContext.restart() {
        val current = getById(host._id) ?: throw RequestError("无此地图")
        sendCommand("stop")
        clearShutFlag(current._id)
        DockerService.restart(host._id.str)
    }

    suspend fun HostContext.sendCommand(command: String) {
        val hostId = host._id
        val normalized = command.trimEnd()
        if (normalized.isBlank()) throw RequestError("命令不能为空")

        val session = hostStates[hostId]?.session ?: throw RequestError("地图未处于游玩状态")

        val message = WsMessage(
            reqId,
            channel = WsMessage.Channel.Command,
            data = normalized
        )

        try {
            session.send(Frame.Text(message.json))
            reqId++
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

    val Host.status: HostStatus
        get() {
            val status = DockerService.getContainerStatus(_id.str)
            if (status == HostStatus.STARTED && hostStates[_id]?.session != null) {
                return HostStatus.PLAYABLE
            }
            return status
        }

    suspend fun findByModpackVersion(modpackId: ObjectId, verName: String): List<Host> {
        return dbcl.find(
            and(
                eq("modpackId", modpackId),
                eq("packVer", verName)
            )
        ).toList()
    }

    suspend fun findByModpack(modpackId: ObjectId): List<Host> {
        return dbcl.find(
            and(
                eq("modpackId", modpackId),
            )
        ).toList()
    }

    // Get logs by range: [startLine, endLine), 0 means newest line
    suspend fun HostContext.getLog(startLine: Int = 0, needLines: Int): String {
        if (needLines > 200) throw RequestError("行数太多")
        val hostId = host._id
        return DockerService.getLog(hostId.str, startLine, startLine + needLines)
    }

    // ---------- Streaming Helper (still needs ApplicationCall for SSE) ----------
    suspend fun HostContext.listenLogs(session: ServerSSESession) {
        val hostId = host._id
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


    fun Host.hasMember(id: ObjectId): Boolean {
        return members.any { it.id == id }
    }

    suspend fun RAccount.ownHosts() = HostService.getByOwner(_id)


    suspend fun RAccount.listHostLobby(
        page: Int,
        pageSize: Int = HOSTS_PER_PAGE,
        myOnly: Boolean = false
    ): List<Host.BriefVo> {
        val safePage = page.coerceAtLeast(0)
        val safeSize = pageSize.coerceIn(1, 100)
        val hosts = dbcl.find()
            .sort(com.mongodb.client.model.Sorts.descending("_id"))
            .skip(safePage * safeSize)
            .limit(safeSize)
            .toList()

        if (hosts.isEmpty()) return emptyList()

        val requesterId = _id
        val visibleHosts = hosts.filter { host ->
            //官服永远显示
            if (PlayerService.getName(host.ownerId) == "davickk")
                return@filter true
            //不是受邀成员 只显示能玩的
            if (!myOnly) {
                val status = host.status
                if (status != HostStatus.PLAYABLE && status != HostStatus.STARTED && status != HostStatus.PAUSED) {
                    return@filter false
                }
            }
            val isMember = host.ownerId == requesterId || host.members.any { it.id == requesterId }
            if (myOnly && !isMember) return@filter false
            if (host.whitelist && !isMember) return@filter false
            true
        }

        if (visibleHosts.isEmpty()) return emptyList()


        return coroutineScope {
            visibleHosts.map { host ->
                async {
                    val modpack = ModpackService.getById(host.modpackId)
                    val onlinePlayers = host.getOnlinePlayers()
                    Host.BriefVo(
                        _id = host._id,
                        intro = host.intro,
                        name = host.name,
                        ownerId = host.ownerId,
                        modpackName = modpack?.name ?: "未知整合包",
                        iconUrl = modpack?.iconUrl,
                        packVer = host.packVer,
                        port = host.port,
                        onlinePlayerIds = onlinePlayers
                    )
                }
            }.awaitAll()
        }
    }

    // List all hosts belonging to a team
    suspend fun getByOwner(uid: ObjectId): List<Host> =
        dbcl.find(eq("ownerId", uid)).toList()


    suspend fun getByPort(port: Int): Host? = dbcl.find(eq("port", port)).firstOrNull()

    suspend fun findByWorld(worldId: ObjectId): Host? =
        dbcl.find(eq("worldId", worldId)).firstOrNull()

    suspend fun getById(id: ObjectId): Host? = dbcl.find(eq("_id", id)).firstOrNull()
    /*

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
            DockerService.createContainer(host.port, containerName, newWorldId?.str, host.imageRef(),host.containerEnv)
            dbcl.updateOne(eq("_id", host._id), set(Host::worldId.name, newWorldId))
            if (newWorldId != null && wasRunning) {
                DockerService.start(containerName)
            }
            clearShutFlag(host._id)
        }
    */

    suspend fun stopIdleHosts() {
        runIdleMonitorTick(forceStop = true)
    }


    private fun Host.overlayRootDir(): File = HOSTS_DIR.resolve(_id.str)


    suspend fun HostContext.delMember() {
        if (targetMember.role.level <= Role.ADMIN.level) {
            throw RequestError("无法踢出管理员")
        }
        dbcl.updateOne(
            eq("_id", host._id),
            Updates.pull(Host::members.name, eq("id", targetMember.id))
        )
    }

    suspend fun HostContext.transferOwnership() {
        val current = getById(host._id) ?: throw RequestError("无此地图")
        val recipient = targetMember
        if (current.ownerId == recipient.id) throw RequestError("不能转给自己")

        val previousOwner = current.members.find { it.id == current.ownerId }
            ?: throw RequestError("当前拥有者不在成员列表")
        val hasRecipient = current.members.any { it.id == recipient.id }
        if (!hasRecipient) throw RequestError("目标成员不在主机成员列表中")

        val updatedMembers = current.members.map { member ->
            when (member.id) {
                previousOwner.id -> member.copy(role = Role.ADMIN)
                recipient.id -> member.copy(role = Role.OWNER)
                else -> member
            }
        }

        dbcl.updateOne(
            eq("_id", current._id),
            combine(
                set(Host::ownerId.name, recipient.id),
                set(Host::members.name, updatedMembers)
            )
        )

    }

    suspend fun HostContext.addMember(qq: String) {
        val target = PlayerService.getByQQ(qq) ?: throw RequestError("无此账号")
        if (host.hasMember(target._id)) {
            throw RequestError("该用户已是成员")
        }
        dbcl.updateOne(
            eq("_id", host._id),
            Updates.push(Host::members.name, Host.Member(target._id, Role.MEMBER))
        )
    }

    suspend fun HostContext.setRole(role: Role) {
        if (targetMember.role == role) {
            throw RequestError("角色未更改")
        }
        if (targetMember.role == Role.OWNER) {
            throw RequestError("无法更改拥有者角色")
        }
        if (role == Role.OWNER) {
            throw RequestError("请使用转移拥有者来指定新的拥有者")
        }
        dbcl.updateOne(
            eq("_id", host._id),
            Updates.combine(
                Updates.set("${Host::members.name}.$[elem].${Host.Member::role.name}", role),
            ),
            UpdateOptions().arrayFilters(
                listOf(
                    Document("elem.id", targetMember.id),
                )
            )
        )

    }
}

