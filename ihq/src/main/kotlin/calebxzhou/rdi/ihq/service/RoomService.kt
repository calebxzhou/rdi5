package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.DB
import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.model.FirmSection
import calebxzhou.rdi.ihq.model.FirmSectionData
import calebxzhou.rdi.ihq.model.RBlockState
import calebxzhou.rdi.ihq.model.Room
import calebxzhou.rdi.ihq.net.e500
import calebxzhou.rdi.ihq.net.got
import calebxzhou.rdi.ihq.net.ok
import calebxzhou.rdi.ihq.net.uid
import calebxzhou.rdi.ihq.util.serdesJson
import com.mongodb.client.model.*
import com.mongodb.client.model.Filters.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.bson.Document
import org.bson.types.ObjectId

object RoomService {
    val dbcl = DB.getCollection<Room>("room")

    init {
        runBlocking {
            dbcl.createIndex(
                Indexes.ascending(
                    "${Room::firmSections.name}.${FirmSection::dimension.name}",
                    "${Room::firmSections.name}.${FirmSection::chunkPos.name}",
                    "${Room::firmSections.name}.${FirmSection::sectionY.name}"
                )
            )
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
        val bstates = serdesJson.decodeFromString<List<RBlockState>>(params got "bstates")

        val iid = dbcl.insertOne(
            Room(
                name = roomName,
                members = listOf(
                    Room.Member(
                        call.uid,
                        true
                    )
                ),
                blockStates = bstates
            )
        ).insertedId?.asObjectId()?.value?.toString() ?: let {
            call.e500("创建房间失败：iid=null")
            return
        }
        //创建房间逻辑

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
        val iid = ObjectId(params got "iid")
        getById(iid)?.let { island ->
            //  rconPost("spectator ${call.uid}")
            //   rconPost("tp ${call.uid} posL rdi:i_${island._id},${island.homePos}")
            call.ok()
        } ?: throw RequestError("没这个岛")
    }

    suspend fun addFirmSection(call: ApplicationCall) {
        val room = getJoinedRoom(call.uid) ?: let {
            throw RequestError("没房")
        }
        val params = call.receiveParameters()
        val roomId = room._id
        val dimension = params got "dimension"
        val chunkPos = (params got "chunkPos").toInt()
        val sectionY = (params got "sectionY").toByte()
        if(getFirmSectionsSize(roomId)>32){
            throw RequestError("持久子区块数量已达上限")
        }
        findFirmSection(dimension, chunkPos, sectionY)?.let {
            throw RequestError("持久子区块已存在: $it" )
        }
        val sectionId = ObjectId()
        val section = FirmSection(
            id = sectionId,
            dimension = dimension,
            chunkPos = chunkPos,
            sectionY = sectionY
        )
        val data = FirmSectionData(
            _id = sectionId,
            roomId = roomId,
        )
        LevelService.sectionDataCol.insertOne(data).let { insertResult ->
            if (insertResult.wasAcknowledged().not()) {
                throw RequestError("添加持久子区块数据失败")
            }
        }
        val updateResult = dbcl.updateOne(
            eq("_id", roomId),
            Updates.push(Room::firmSections.name, section),
            UpdateOptions().upsert(true)
        )
        if (updateResult.modifiedCount == 0L && updateResult.upsertedId == null) {
            throw RequestError("添加持久子区块失败")
        }
        call.ok("$sectionId")
    }
    suspend fun removeFirmSection(call: ApplicationCall) {
        val params = call.receiveParameters()
        val room = getJoinedRoom(call.uid) ?: let {
            throw RequestError("没房")
        }
        val roomId = room._id
        val sectionId = ObjectId(params got "sectionId")
        val section = findFirmSection(
            params got "dimension",
            (params got "chunkPos").toInt(),
            (params got "sectionY").toByte()
        ) ?: let {
            throw RequestError("持久子区块不存在")
        }
        if (section.id != sectionId) {
            throw RequestError("持久子区块id不匹配")
        }
        dbcl.updateOne(
            eq("_id", roomId),
            Updates.pull(Room::firmSections.name, eq(FirmSection::id.name, sectionId))
        ).let { updateResult ->
            if (updateResult.modifiedCount == 0L) {
                throw RequestError("删除持久子区块失败")
            }
        }
        LevelService.sectionDataCol.deleteOne(eq("_id", sectionId)).let { deleteResult ->
            if (deleteResult.deletedCount == 0L) {
                throw RequestError("删除持久子区块数据失败")
            }
        }
        call.ok()
    }
    suspend fun getFirmSectionsSize(roomId: ObjectId): Int {
        val pipeline = listOf(
            Aggregates.match(eq("_id", roomId)),
            Aggregates.project(Projections.fields(
                Projections.excludeId(),
                Projections.computed("firmSectionsSize", Document($$"$size", $$"$firmSections"))
            ))
        )
        val flow = dbcl.aggregate<Document>(pipeline)
        val result = flow.first()
        return result.getInteger("firmSectionsSize")

    }
    suspend fun getFirmSectionData(call: ApplicationCall){
        val params = call.receiveParameters()
        val room = getJoinedRoom(call.uid) ?: let {
            throw RequestError("没房")
        }
        val roomId = room._id
        val sectionId = ObjectId(params got "sectionId")
        val section = findFirmSection(
            params got "dimension",
            (params got "chunkPos").toInt(),
            (params got "sectionY").toByte()
        ) ?: let {
            throw RequestError("持久子区块不存在")
        }
        LevelService.sectionDataCol.find(
            eq("_id", sectionId)
        ).firstOrNull()?.let { sectionData ->
            call.ok(serdesJson.encodeToString(sectionData))
        } ?: throw RequestError("持久子区块数据不存在")

    }
    suspend fun findFirmSection(
        dimension: String,
        chunkPos: Int,
        sectionY: Byte
    ): FirmSection? {
        val pipeline = listOf(
            Aggregates.match(
                Filters.elemMatch(
                    Room::firmSections.name,
                    and(
                        eq(FirmSection::dimension.name, dimension),
                        eq(FirmSection::chunkPos.name, chunkPos),
                        eq(FirmSection::sectionY.name, sectionY)
                    )
                )
            ),
            Aggregates.unwind($$"$$${Room::firmSections.name}"),
            Aggregates.match(
                and(
                    eq("${Room::firmSections.name}.${FirmSection::dimension.name}", dimension),
                    eq("${Room::firmSections.name}.${FirmSection::chunkPos.name}", chunkPos),
                    eq("${Room::firmSections.name}.${FirmSection::sectionY.name}", sectionY)
                )
            ),
            Aggregates.project(
                Projections.fields(
                    Projections.excludeId(),
                    Projections.computed(
                        FirmSection::id.name,
                        $$"$$${Room::firmSections.name}.$${FirmSection::id.name}"
                    ),
                    Projections.computed(
                        FirmSection::dimension.name,
                        $$"$$${Room::firmSections.name}.$${FirmSection::dimension.name}"
                    ),
                    Projections.computed(
                        FirmSection::chunkPos.name,
                        $$"$$${Room::firmSections.name}.$${FirmSection::chunkPos.name}"
                    ),
                    Projections.computed(
                        FirmSection::sectionY.name,
                        $$"$$${Room::firmSections.name}.$${FirmSection::sectionY.name}"
                    )
                )
            )
        )
        return dbcl.aggregate<FirmSection>(pipeline).firstOrNull()
    }
}