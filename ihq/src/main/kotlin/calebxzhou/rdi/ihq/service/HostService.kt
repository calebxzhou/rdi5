package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.DB
import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.model.Host
import calebxzhou.rdi.ihq.model.ServerStatus
import calebxzhou.rdi.ihq.net.ok
import calebxzhou.rdi.ihq.net.param
import calebxzhou.rdi.ihq.net.response
import calebxzhou.rdi.ihq.net.uid
import calebxzhou.rdi.ihq.service.TeamService.addHost
import calebxzhou.rdi.ihq.service.TeamService.delHost
import calebxzhou.rdi.ihq.util.str
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Updates
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId
import java.util.concurrent.ConcurrentLinkedQueue

// ---------- Routing DSL (mirrors teamRoutes style) ----------
fun Route.roomRoutes() = route("/host") {
    post("/create") {
        HostService.create(uid, ObjectId(param("modpackId")), param("packVer"), ObjectId(param("worldId")))
        ok()
    }
    post("/delete") {
        HostService.delete(uid, ObjectId(param("hostId")))
        ok()
    }
    get("/log") { response(data=HostService.getLog(uid, ObjectId(param("hostId")),param("skipLines").toInt())) }
    get("/log/stream") { HostService.listenLogs(call) }
    get("/list") {
        response(data = HostService.dbcl.find().map { it._id.toString() to it.name }.toList())
    }
    get("/status") {
        response(
            data = HostService.getServerStatus(ObjectId(param("hostId"))).toString()
        )
    }
    post("/start") {
        HostService.start(uid, ObjectId(param("hostId")))
        ok()
    }
    post("/stop") {
        HostService.stop(uid, ObjectId(param("hostId")))
        ok()
    }
  /*  post("/update") {
        HostService.update(
            uid,
            call.receiveParameters()["image"] ?: DEFAULT_IMAGE
        ); response()
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

    suspend fun create(uid: ObjectId, modpackId: ObjectId, packVer: String, worldId: ObjectId) {
        val player = PlayerService.getById(uid) ?: throw RequestError("玩家未注册")
        val team = TeamService.getOwn(uid) ?: throw RequestError("你不是队长")
        if (team.hosts().size > 3) throw RequestError("主机最多3个")
        val port = allocateRoomPort()
        val host = Host(
            name = player.name + "的房间",
            teamId = team._id,
            modpackId = modpackId,
            worldId = worldId,
            port = port,
            packVer = packVer
        )
        dbcl.insertOne(host)
        team.addHost(host._id)
        DockerService.createContainer(port, host._id.toString(), worldId.toString(), modpackId.toString())
        DockerService.start(host._id.str)
    }

    suspend fun delete(uid: ObjectId, hostId: ObjectId) {
        val team = TeamService.getOwn(uid) ?: throw RequestError("你不是队长")
        if (!team.hasHost(hostId)) throw RequestError("此主机不属于你的团队")
        dbcl.deleteOne(eq("_id", hostId))
        team.delHost(hostId)
        DockerService.deleteContainer(hostId.str)
    }


    suspend fun update(uid: ObjectId, hostId: ObjectId, packVer: String, modpackId: ObjectId) {
        val team = TeamService.getOwn(uid) ?: throw RequestError("你不是队长")
        if (!team.hasHost(hostId)) throw RequestError("此主机不属于你的团队")
        val host = getById(hostId) ?: throw RequestError("无此主机")
        DockerService.stop(hostId.str)
        DockerService.deleteContainer(hostId.str)
        DockerService.createContainer(host.port, hostId.str, host.worldId.str, modpackId.str + ":" + packVer)
        dbcl.updateOne(eq("_id", host._id), Updates.set(Host::packVer.name, packVer))
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

    suspend fun getServerStatus(hostId: ObjectId): ServerStatus {
        return getById(hostId)?._id?.let {
            DockerService.getContainerStatus(
                it.str
            )
        } ?: ServerStatus.UNKNOWN
    }
    //skipLines=从后往前跳过多少行
    suspend fun getLog(uid: ObjectId,hostId: ObjectId, skipLines: Int): String {
        val host = getById(hostId) ?: throw RequestError("无此主机")
        val team = TeamService.get(host.teamId) ?: throw RequestError("无此团队")
        if (!team.hasHost(hostId)) throw RequestError("此主机不属于你的团队")
        if (!team.isOwnerOrAdmin(uid)) throw RequestError("无权限")

        return DockerService.getLog(room.containerId, skipLines)
    }

    // List all hosts belonging to a team
    suspend fun listByTeam(teamId: ObjectId): List<Host> =
        dbcl.find(eq("teamId", teamId)).toList()

    suspend fun belongsToTeam(hostId: ObjectId, teamId: ObjectId): Boolean =
        dbcl.find(and(eq("_id", hostId), eq("teamId", teamId)))
            .projection(org.bson.Document("_id", 1))
            .limit(1)
            .firstOrNull() != null
    suspend fun getById(id: ObjectId): Host? = dbcl.find(eq("_id", id)).firstOrNull()

    // ---------- Streaming Helper (still needs ApplicationCall for SSE) ----------
    suspend fun listenLogs(uid: ObjectId,hostId: ObjectId,call: ApplicationCall) {
        val host = getById(hostId) ?: throw RequestError("无此主机")
        val team = TeamService.get(host.teamId) ?: throw RequestError("无此团队")
        if (!team.hasHost(hostId)) throw RequestError("此主机不属于你的团队")
        if (!team.isOwnerOrAdmin(uid)) throw RequestError("无权限")

        call.response.cacheControl(CacheControl.NoCache(null))
        call.respondBytesWriter(contentType = ContentType.Text.EventStream) {
            val writer = this
            val pending = ConcurrentLinkedQueue<String>()
            fun enqueue(event: String? = null, data: String) {
                val sb = StringBuilder()
                if (event != null) sb.append("event: ").append(event).append('\n')
                data.lines().forEach { line -> sb.append("data: ").append(line).append('\n') }
                sb.append('\n')
                pending.add(sb.toString())
            }
            enqueue("hello", "start streaming")
            DockerService.listenLog(
                hostId.str,
                tail = 100,
                onLine = { line -> enqueue(data = line) },
                onError = { t -> enqueue("error", t.message ?: "error") },
                onFinished = { enqueue("done", "finished") }
            ).use {
                var lastHeartbeat = System.currentTimeMillis()
                while (true) {
                    var wrote = false
                    while (true) {
                        val msg = pending.poll() ?: break
                        writer.writeFully(msg.toByteArray())
                        wrote = true
                    }
                    val now = System.currentTimeMillis()
                    if (now - lastHeartbeat > 15000) {
                        writer.writeFully("data: 💓\n\n".toByteArray())
                        lastHeartbeat = now
                        wrote = true
                    }
                    if (wrote) flush()
                    delay(500)
                }
            }
        }
    }
}

