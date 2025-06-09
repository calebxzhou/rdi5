package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.DB
import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.model.Room
import calebxzhou.rdi.ihq.net.e500
import calebxzhou.rdi.ihq.net.got
import calebxzhou.rdi.ihq.net.ok
import calebxzhou.rdi.ihq.net.uid
import calebxzhou.rdi.ihq.util.*
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import io.ktor.server.application.*
import io.ktor.server.request.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.bson.Document
import org.bson.types.ObjectId

object RoomService {
    val dbcl = DB.getCollection<Room>("room")
    //玩家已创建的岛屿
    suspend fun getOwnRoom(uid: ObjectId): Room? = dbcl.find(
        elemMatch(
            "members", and(
                eq("id", uid),
                eq("isOwner", true)
            )
        )
    ).firstOrNull()

    //玩家所在的岛屿
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
        if (getJoinedRoom(call.uid) != null) {
            throw RequestError("已有房间，必须退出/删除方可创建新的")
        }
        val player = PlayerService.getById(call.uid)
        if (player == null) {
            throw RequestError("玩家未注册")
        }
        val roomName = player.name + "的房间"
        Document()
        val iid = dbcl.insertOne(
            Room(
                name = roomName,
                members = listOf(
                    Room.Member(
                        call.uid,
                        true
                    )
                )
            )
        ).insertedId?.asObjectId()?.value?.toString() ?: let {
            call.e500("创建岛屿失败：iid=null")
            return
        }
        //rconPost("createIsland $iid")
        call.ok(iid)
    }

    suspend fun delete(call: ApplicationCall) {
        val island = getOwnRoom(call.uid) ?: let {
            throw RequestError("没岛")
        }
        dbcl.deleteOne(eq("_id", island._id))
        //rconPost("deleteIsland ${island._id}")
        //rconPost("resetPlayer ${call.uid}")

        //  向mc服务器发送rcon指令 清存档+删岛
        call.ok()
    }

    //回到自己拥有/加入的岛屿
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

    //退出岛屿
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

    //邀请玩家加入岛屿
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
        val uid2 = PlayerService.getByQQ(qq) ?:  throw RequestError("此玩家不存在")
        if (getJoinedRoom(uid2._id) != null) {
            throw RequestError("他有岛")
        }
        dbcl.updateOne(
            eq("_id", island._id),
            Updates.push("members", Room.Member(uid2._id, false))
        )
        call.ok(uid2.name+",QQ"+uid2.qq)
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
    suspend fun list(call: ApplicationCall){
        val islands = dbcl.find().map { it._id.toString() to it.name }.toList()
        call.ok(serdesJson.encodeToString(islands))
    }
    suspend fun visit(call: ApplicationCall) {
        val params = call.receiveParameters()
        val iid = ObjectId(params got "iid")
        getById(iid)?.let { island ->
          //  rconPost("spectator ${call.uid}")
         //   rconPost("tp ${call.uid} posL rdi:i_${island._id},${island.homePos}")
        call.ok()
        }?:throw RequestError("没这个岛")
    }
}