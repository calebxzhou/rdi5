package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.DB
import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.model.Room
import calebxzhou.rdi.ihq.net.e500
import calebxzhou.rdi.ihq.net.got
import calebxzhou.rdi.ihq.net.initGetParams
import calebxzhou.rdi.ihq.net.ok
import calebxzhou.rdi.ihq.net.randomPort
import calebxzhou.rdi.ihq.net.uid
import calebxzhou.rdi.ihq.service.DockerService.asVolumeName
import calebxzhou.rdi.ihq.util.serdesJson
import com.mongodb.client.model.*
import com.mongodb.client.model.Filters.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.http.*
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId

object RoomService {
    val dbcl = DB.getCollection<Room>("room")

    init {
        runBlocking {
            /*dbcl.createIndex(
                Indexes.ascending(
                    "${Room::firmSections.name}.${FirmSection::dimension.name}",
                    "${Room::firmSections.name}.${FirmSection::chunkPos.name}",
                    "${Room::firmSections.name}.${FirmSection::sectionY.name}"
                )
            )*/
            dbcl.createIndex(
                Indexes.ascending("${Room::members.name}.${Room.Member::id.name}"),
            )
        }
    }

    //玩家已创建的房间
    suspend fun getOwnRoom(uid: ObjectId): Room? = dbcl.find(
        elemMatch(
            "members", and(
                eq("id", uid),
                eq("isOwner", true)
            )
        )
    ).firstOrNull()

    //玩家所在的房间
    suspend fun getJoinedRoom(uid: ObjectId): Room? = dbcl.find(
        elemMatch(
            "members", and(
                eq("id", uid)
            )
        )
    ).firstOrNull()

    suspend fun my(call: ApplicationCall) {
        getJoinedRoom(call.uid)?.let {
            call.ok(serdesJson.encodeToString(it))
        } ?: call.ok("0")
    }

    //建岛
    suspend fun create(call: ApplicationCall) {
        val params = call.receiveParameters()
        if (getJoinedRoom(call.uid) != null) {
            throw RequestError("已有房间，必须退出/删除方可创建新的")
        }
        val player = PlayerService.getById(call.uid) ?: throw RequestError("玩家未注册")
        val roomName = player.name + "的房间"
        //val bstates = serdesJson.decodeFromString<List<RBlockState>>(params got "bstates")
        val roomId= ObjectId()
        val port = randomPort
        val contId = DockerService.create(port,roomId.toString(),roomId.asVolumeName,"abc")
        val room = Room(
            roomId,
            name = roomName,
            port=port,
            members = listOf(
                Room.Member(
                    call.uid,
                    true
                )
            ),
            containerId = contId,
        )
        dbcl.insertOne(
            room
        ).insertedId?.asObjectId()?.value?.toString() ?: let {
            call.e500("创建房间失败：rid=null")
            return
        }
        DockerService.start(contId)
        call.ok(serdesJson.encodeToString(room))
    }

    suspend fun delete(call: ApplicationCall) {
        val room = getOwnRoom(call.uid) ?: let {
            throw RequestError("没岛")
        }

        try {
            // First delete from database - this is the most important operation
            dbcl.deleteOne(eq("_id", room._id))

            // Then try to clean up Docker resources (don't let Docker errors prevent room deletion)
            try {
                DockerService.delete("data_${room._id}", room.containerId)
            } catch (e: Exception) {
                e.printStackTrace()
                // Log Docker cleanup failure but don't fail the entire operation
                println("Warning: Failed to clean up Docker resources for room ${room._id}: ${e.message}")
            }

            call.ok()
        } catch (e: Exception) {
            println("Error deleting room: ${e.message}")
            throw RequestError("删除房间失败: ${e.message}")
        }
    }

    //回到自己拥有/加入的房间
    suspend fun home(call: ApplicationCall) {
        goHome(call.uid)
        call.ok()
    }

    suspend fun goHome(uid: ObjectId) {
        val island = getJoinedRoom(uid) ?: let {
            throw RequestError("没岛")
        }
        // mc-rcon 传送
        //rconPost("tp ${uid} posL rdi:i_${island._id},${island.homePos}")
        //rconPost("survival $uid")
    }

    //设传送点
    suspend fun sethome(call: ApplicationCall) {
        val params = call.receiveParameters()
        val homePos = (params got "pos").toLong()
        val island = getOwnRoom(call.uid) ?: let {
            throw RequestError("必须岛主来做")
        }
        //todo 客户端检查脚下实心方块
        dbcl.updateOne(
            eq("_id", island._id),
            Updates.set("homePos", homePos)
        )
        call.ok()
    }

    //退出房间
    suspend fun quit(call: ApplicationCall) {
        val island = getJoinedRoom(call.uid) ?: let {
            throw RequestError("没岛")
        }
        if (island.owner.id == call.uid) {
            throw RequestError("你是岛主，只能删除")
        }
        dbcl.updateOne(
            eq("_id", island._id),
            Updates.pull("members", eq("id", call.uid))
        )
        //删玩家档
        //rconPost("resetPlayer ${call.uid}")
        call.ok()
    }

    //邀请玩家加入房间
    suspend fun invite(call: ApplicationCall) {
        val params = call.receiveParameters()
        val uid1 = call.uid
        val uid2 = ObjectId(params got "uid2")

        val island = getOwnRoom(uid1) ?: let {
            throw RequestError("你没岛")
        }
        if (getJoinedRoom(uid2) != null) {
            throw RequestError("他有岛")
        }
        dbcl.updateOne(
            eq("_id", island._id),
            Updates.push("members", Room.Member(uid2, false))
        )
        call.ok()
    }

    suspend fun inviteQQ(call: ApplicationCall) {
        val params = call.receiveParameters()
        val uid1 = call.uid
        val qq = params got "qq"

        val island = getOwnRoom(uid1) ?: let {
            throw RequestError("你没岛")
        }
        val uid2 = PlayerService.getByQQ(qq) ?: throw RequestError("此玩家不存在")
        if (getJoinedRoom(uid2._id) != null) {
            throw RequestError("他有岛")
        }
        dbcl.updateOne(
            eq("_id", island._id),
            Updates.push("members", Room.Member(uid2._id, false))
        )
        call.ok(uid2.name + ",QQ" + uid2.qq)
    }

    //踢出
    suspend fun kick(call: ApplicationCall) {
        val params = call.receiveParameters()
        val uid1 = call.uid
        val uid2 = ObjectId(params got "uid2")
        if (uid1 == uid2) {
            throw RequestError("不能踢自己")
        }
        val island = getOwnRoom(uid1) ?: let {
            throw RequestError("你没岛")
        }
        if (!island.hasMember(uid2)) {
            throw RequestError("他不是岛员")
        }
        dbcl.updateOne(
            eq("_id", island._id),
            Updates.pull("members", eq("id", uid2))
        )
        // 删除对方存档
        //rconPost("resetPlayer $uid2")
        call.ok()
    }

    //转让
    suspend fun transfer(call: ApplicationCall) {
        val params = call.receiveParameters()
        val uid1 = call.uid
        val uid2 = ObjectId(params got "uid2")
        if (uid1 == uid2) {
            throw RequestError("不能转给自己")
        }
        val island = getOwnRoom(uid1) ?: let {
            throw RequestError("你没岛")
        }
        if (!island.hasMember(uid2)) {
            throw RequestError("他不是岛员")
        }
        //给对方加上岛主权限
        dbcl.updateOne(
            eq("_id", island._id),
            Updates.set("members.$[element].isOwner", true),
            UpdateOptions().arrayFilters(listOf(eq("element.id", uid2)))
        )
        //给自己去掉岛主权限
        dbcl.updateOne(
            eq("_id", island._id),
            Updates.set("members.$[element].isOwner", false),
            UpdateOptions().arrayFilters(listOf(eq("element.id", uid1)))
        )
        call.ok()
    }


    suspend fun getById(id: ObjectId): Room? = dbcl.find(eq("_id", id)).firstOrNull()
    suspend fun list(call: ApplicationCall) {
        val islands = dbcl.find().map { it._id.toString() to it.name }.toList()
        call.ok(serdesJson.encodeToString(islands))
    }

    suspend fun visit(call: ApplicationCall) {
        val params = call.receiveParameters()
        val rid = ObjectId(params got "rid")
        getById(rid)?.let { island ->
            //  rconPost("spectator ${call.uid}")
            //   rconPost("tp ${call.uid} posL rdi:i_${island._id},${island.homePos}")
            call.ok()
        } ?: throw RequestError("没这个岛")
    }

    suspend fun isServerStarted(call: ApplicationCall) {
        val params = call.initGetParams()
        val rid = ObjectId(params got "rid")
        getById(rid)?.let { room ->
            call.ok(DockerService.isStarted(room.containerId).toString())
        } ?: throw RequestError("没这个岛")
    }
    suspend fun getServerLog(call: ApplicationCall){
        val params = call.initGetParams()
        val page = params got "page"
        getJoinedRoom(call.uid)?.let { room ->
            call.ok(DockerService.getLog(room.containerId,page.toInt()))
        } ?: throw RequestError("没这个岛")
    }
    suspend fun streamServerLogSse(call: ApplicationCall){
        val room = getJoinedRoom(call.uid) ?: throw RequestError("没这个岛")
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
    suspend fun startServer(call: ApplicationCall) {
        val room = getJoinedRoom(call.uid) ?: throw RequestError("没岛")
        DockerService.start(room.containerId)
        call.ok()
    }
    suspend fun stopServer(call: ApplicationCall) {
        val room = getJoinedRoom(call.uid) ?: throw RequestError("没岛")
        DockerService.stop(room.containerId)
        call.ok()
    }
    suspend fun getServerStatus(call: ApplicationCall) {
        val room = getJoinedRoom(call.uid) ?: throw RequestError("没岛")
//todo
        call.ok(DockerService.getStatus(room.containerId).toString())
    }
}