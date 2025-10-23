package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.DB
import calebxzhou.rdi.ihq.DEFAULT_MODPACK_ID
import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.model.Host
import calebxzhou.rdi.ihq.model.imageRef
import calebxzhou.rdi.ihq.model.ServerStatus
import calebxzhou.rdi.ihq.net.ok
import calebxzhou.rdi.ihq.net.param
import calebxzhou.rdi.ihq.net.paramNull
import calebxzhou.rdi.ihq.net.response
import calebxzhou.rdi.ihq.net.uid
import calebxzhou.rdi.ihq.service.TeamService.addHost
import calebxzhou.rdi.ihq.service.TeamService.delHost
import calebxzhou.rdi.ihq.util.str
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import io.ktor.http.CacheControl
import io.ktor.server.response.cacheControl
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlin.time.Duration.Companion.seconds
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
            paramNull("worldId")?.let {  ObjectId(it)}
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
        response(data = HostService.getLog(uid, hostId, paramNull("startLine")?.toInt()?:0, lines))
    }
    sse("/{hostId}/log/stream") {
        val hostId = ObjectId(call.param("hostId"))
        HostService.listenLogs(call.uid, hostId, this)
    }
    get("/") {
        TeamService.getJoinedTeam(uid)
            ?.let { HostService.listByTeam(it._id) }
            ?.let { response(data = it) }
            ?:response(data = emptyList<Host>())
    }
    /* todo 以后再支持 for 自由选择host
    get("/all") {
        response(data = HostService.dbcl.find().map { it._id.toString() to it.name }.toList())
    }*/

}

object HostService {

    val dbcl = DB.getCollection<Host>("host")

    private const val PORT_START = 50000

    private const val PORT_END_EXCLUSIVE = 60000

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

    // ---------- Core Logic (no ApplicationCall side-effects) ----------

    suspend fun create(uid: ObjectId, modpackId: ObjectId, packVer: String, worldId: ObjectId?) {
        val player = PlayerService.getById(uid) ?: throw RequestError("玩家未注册")
        val team = TeamService.getOwn(uid) ?: throw RequestError("你不是队长")
        if (team.hosts().size > 1) throw RequestError("内测阶段只能有一个主机")
        val world = worldId?.let {
            if (findByWorld(it) != null) throw RequestError("此存档已被其他主机占用")

            WorldService.getById(it) ?: throw RequestError("无此存档")
        }?: WorldService.create(uid,null,modpackId)
        if (world.teamId != team._id) throw RequestError("存档不属于你的团队")

        val port = allocateRoomPort()
        val host = Host(
            name = team.name + "的主机"+ (team.hosts().size+1),
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
    }

    suspend fun delete(uid: ObjectId, hostId: ObjectId) {
        val team = TeamService.getOwn(uid) ?: throw RequestError("你不是队长")
        if (!team.hasHost(hostId)) throw RequestError("此主机不属于你的团队")
        val host = getById(hostId) ?: throw RequestError("无此主机")
        dbcl.deleteOne(eq("_id", hostId))
        team.delHost(hostId)
        DockerService.deleteContainer(hostId.str)
    }


    suspend fun update(uid: ObjectId, hostId: ObjectId,  packVer: String?) {
        val packVer = packVer?:"latest"
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
    }

    suspend fun stop(uid: ObjectId, hostId: ObjectId) {
        val host = getById(hostId) ?: throw RequestError("无此主机")
        val team = TeamService.get(host.teamId) ?: throw RequestError("无此团队")
        if (!team.hasHost(hostId)) throw RequestError("此主机不属于你的团队")
        if (!team.isOwnerOrAdmin(uid)) throw RequestError("无权限")
        DockerService.stop(hostId.str)
    }
    suspend fun restart(uid: ObjectId, hostId: ObjectId) {
        val host = getById(hostId) ?: throw RequestError("无此主机")
        val team = TeamService.get(host.teamId) ?: throw RequestError("无此团队")
        if (!team.hasHost(hostId)) throw RequestError("此主机不属于你的团队")
        if (!team.isOwnerOrAdmin(uid)) throw RequestError("无权限")
        DockerService.restart(hostId.str)
    }

    suspend fun getServerStatus(hostId: ObjectId): ServerStatus {
        return getById(hostId)?._id?.let {
            DockerService.getContainerStatus(
                it.str
            )
        } ?: ServerStatus.UNKNOWN
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
    }
}

