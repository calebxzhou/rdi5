package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.DB
import calebxzhou.rdi.ihq.DEFAULT_IMAGE
import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.model.Host
import calebxzhou.rdi.ihq.net.ok
import calebxzhou.rdi.ihq.net.param
import calebxzhou.rdi.ihq.net.response
import calebxzhou.rdi.ihq.net.uid
import calebxzhou.rdi.ihq.service.DockerService.asVolumeName
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.UpdateOptions
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
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId
import java.util.concurrent.ConcurrentLinkedQueue

// ---------- Routing DSL (mirrors teamRoutes style) ----------
fun Route.roomRoutes() = route("/host") {
    post("/create") {
        HostService.create(uid)
        ok()
    }
    post("/delete") {
        HostService.deleteRoomOf(uid)
        ok()
    }
    post("/transfer") { HostService.transferOwnership(uid, ObjectId(param("uid2"))); response() }
    get("/log") { response(HostService.serverLog(uid, param("page").toInt())) }
    get("/log/stream") { HostService.streamServerLogSse(call) }
    get("/list") {
        val islands = HostService.dbcl.find().map { it._id.toString() to it.name }.toList()
        response(data = islands)
    }
    post("/visit") {
        val rid = ObjectId(param("rid")); HostService.getById(rid) ?: throw RequestError("没这个岛"); response()
    }
    route("/server") {
        get("/status") { response(HostService.serverStatus(uid)) }
        post("/start") { HostService.startServer(uid); response() }
        post("/stop") { HostService.stopServer(uid); response() }
        post("/update") { HostService.updateServer(uid, call.receiveParameters()["image"] ?: DEFAULT_IMAGE); response() }
    }
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

    //玩家已创建的房间
    suspend fun getOwn(uid: ObjectId): Host? = dbcl.find(
        elemMatch(
            "members", and(
                eq("id", uid),
                eq("isOwner", true)
            )
        )
    ).firstOrNull()

    //玩家所在的房间
    suspend fun getJoined(uid: ObjectId): Host? = dbcl.find(
        elemMatch(
            "members", and(
                eq("id", uid)
            )
        )
    ).firstOrNull()

    // ---------- Core Logic (no ApplicationCall side-effects) ----------

    suspend fun create(uid: ObjectId, image: String = DEFAULT_IMAGE): Host {
        if (getJoined(uid) != null) throw RequestError("已有房间，必须退出/删除方可创建新的")
        val player = PlayerService.getById(uid) ?: throw RequestError("玩家未注册")
        val roomId = ObjectId()
        val port = allocateRoomPort()
        val contId = DockerService.create(port, roomId.toString(), roomId.asVolumeName, image)
        val host = Host(
            roomId,
            name = player.name + "的房间",
            port = port,
            members = listOf(Host.Member(uid, true)),
            image = image,
            containerId = contId,
        )
        dbcl.insertOne(host)
        DockerService.start(contId)
        return host
    }

    suspend fun deleteRoomOf(uid: ObjectId) {
        val room = getOwn(uid) ?: throw RequestError("没岛")
        dbcl.deleteOne(eq("_id", room._id))
        try { DockerService.delete("data_${room._id}", room.containerId) } catch (_: Exception) {}
    }

    suspend fun setHome(uid: ObjectId, pos: Long) {
        val room = getOwn(uid) ?: throw RequestError("必须岛主来做")
        dbcl.updateOne(eq("_id", room._id), Updates.set("homePos", pos))
    }

    suspend fun quitRoom(uid: ObjectId) {
        val room = getJoined(uid) ?: throw RequestError("没岛")
        if (room.owner.id == uid) throw RequestError("你是岛主，只能删除")
        dbcl.updateOne(eq("_id", room._id), Updates.pull("members", eq("id", uid)))
    }

    suspend fun inviteMember(owner: ObjectId, target: ObjectId) {
        val room = getOwn(owner) ?: throw RequestError("你没岛")
        if (getJoined(target) != null) throw RequestError("他有岛")
        dbcl.updateOne(eq("_id", room._id), Updates.push("members", Host.Member(target, false)))
    }

    suspend fun inviteMemberByQQ(owner: ObjectId, qq: String): Host.Member {
        val room = getOwn(owner) ?: throw RequestError("你没岛")
        val target = PlayerService.getByQQ(qq) ?: throw RequestError("此玩家不存在")
        if (getJoined(target._id) != null) throw RequestError("他有岛")
        val member = Host.Member(target._id, false)
        dbcl.updateOne(eq("_id", room._id), Updates.push("members", member))
        return member
    }

    suspend fun kickMember(owner: ObjectId, target: ObjectId) {
        if (owner == target) throw RequestError("不能踢自己")
        val room = getOwn(owner) ?: throw RequestError("你没岛")
        if (!room.hasMember(target)) throw RequestError("他不是岛员")
        dbcl.updateOne(eq("_id", room._id), Updates.pull("members", eq("id", target)))
    }

    suspend fun transferOwnership(owner: ObjectId, target: ObjectId) {
        if (owner == target) throw RequestError("不能转给自己")
        val room = getOwn(owner) ?: throw RequestError("你没岛")
        if (!room.hasMember(target)) throw RequestError("他不是岛员")
        dbcl.updateOne(
            eq("_id", room._id),
            Updates.set("members.$[element].isOwner", true),
            UpdateOptions().arrayFilters(listOf(eq("element.id", target)))
        )
        dbcl.updateOne(
            eq("_id", room._id),
            Updates.set("members.$[element].isOwner", false),
            UpdateOptions().arrayFilters(listOf(eq("element.id", owner)))
        )
    }

    suspend fun updateServer(uid: ObjectId, image: String = DEFAULT_IMAGE) {
        val room = getJoined(uid) ?: throw RequestError("没岛")
        val newId = DockerService.update(room.containerId, image)
        dbcl.updateOne(eq("_id", room._id), Updates.set("containerId", newId))
    }

    suspend fun startServer(uid: ObjectId) {
        val room = getJoined(uid) ?: throw RequestError("没岛")
        DockerService.start(room.containerId)
    }

    suspend fun stopServer(uid: ObjectId) {
        val room = getJoined(uid) ?: throw RequestError("没岛")
        DockerService.stop(room.containerId)
    }

    suspend fun serverStatus(uid: ObjectId): String {
        val room = getJoined(uid) ?: throw RequestError("没岛")
        return DockerService.getStatus(room.containerId).toString()
    }

    suspend fun serverLog(uid: ObjectId, page: Int): String {
        val room = getJoined(uid) ?: throw RequestError("没这个岛")
        return DockerService.getLog(room.containerId, page)
    }

    suspend fun getById(id: ObjectId): Host? = dbcl.find(eq("_id", id)).firstOrNull()
    // ---------- Streaming Helper (still needs ApplicationCall for SSE) ----------
    suspend fun streamServerLogSse(call: ApplicationCall){
        val room = getJoined(call.uid) ?: throw RequestError("没这个岛")
        call.response.cacheControl(CacheControl.NoCache(null))
        call.respondBytesWriter(contentType = ContentType.Text.EventStream) {
            val writer = this
            val pending = ConcurrentLinkedQueue<String>()
            fun enqueue(event: String? = null, data: String) {
                val sb = StringBuilder()
                if(event!=null) sb.append("event: ").append(event).append('\n')
                data.lines().forEach { line -> sb.append("data: ").append(line).append('\n') }
                sb.append('\n')
                pending.add(sb.toString())
            }
            enqueue("hello","start streaming")
            val closer = DockerService.followLog(
                room.containerId,
                tail = 100,
                onLine = { line -> enqueue(data = line) },
                onError = { t -> enqueue("error", t.message ?: "error") },
                onFinished = { enqueue("done","finished") }
            )
            try {
                // Drain queue periodically; also send heartbeat every 15s
                var lastHeartbeat = System.currentTimeMillis()
                while(true){
                    var wrote = false
                    while(true){
                        val msg = pending.poll() ?: break
                        writer.writeFully(msg.toByteArray())
                        wrote = true
                    }
                    val now = System.currentTimeMillis()
                    if(now - lastHeartbeat > 15000){
                        writer.writeFully("data: 💓\n\n".toByteArray())
                        lastHeartbeat = now
                        wrote = true
                    }
                    if(wrote) flush()
                    delay(500)
                }
            } catch (_: Throwable) { }
            finally { try { closer.close() } catch (_: Throwable) {} }
        }
    }
}

