package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.DB
import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.model.Team
import calebxzhou.rdi.ihq.net.err
import calebxzhou.rdi.ihq.net.ok
import calebxzhou.rdi.ihq.net.param
import calebxzhou.rdi.ihq.net.response
import calebxzhou.rdi.ihq.net.uid
import calebxzhou.rdi.ihq.util.displayLength
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Indexes
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.bson.types.ObjectId

fun Route.teamRoutes() = route("/team") {
    get("/{id}") {
        val id = ObjectId(param("id"))
        TeamService.get(id)?.let {
            response(data = it)
        } ?: err("无此团队")
        ok()
    }
    post("/create") {
        TeamService.create(
            uid,
            param("name"),
            param("info")
        )
        ok()
    }
    post("/delete") {
        TeamService.delete(uid)
        ok()
    }
    post("/invite") {

    }
}

object TeamService {
    val dbcl = DB.getCollection<Team>("team")

    init {
        runBlocking {
            RoomService.dbcl.createIndex(
                Indexes.ascending("members.id"),
            )
        }
    }

    //玩家拥有的团队
    suspend fun getOwnTeam(uid: ObjectId): Team? = dbcl.find(
        elemMatch(
            "members", and(
                eq("id", uid),
                eq("role", Team.Role.OWNER)
            )
        )
    ).firstOrNull()

    //玩家所在的团队
    suspend fun getJoinedTeam(uid: ObjectId): Team? = dbcl.find(
        elemMatch(
            "members", and(
                eq("id", uid)
            )
        )
    ).firstOrNull()

    suspend fun hasJoinedTeam(uid: ObjectId): Boolean = dbcl.find(eq("members.id", uid)).firstOrNull() != null

    suspend fun get(id: ObjectId) = dbcl.find(eq("_id", id)).firstOrNull()
    suspend fun create(uid: ObjectId, name: String, info: String) {
        if (hasJoinedTeam(uid)) throw RequestError("未退出当前团队")
        val player = PlayerService.getById(uid) ?: throw RequestError("无此账号")
        if (name.displayLength > 32) throw RequestError("团队名称显示长度>32")
        if (info.displayLength > 512) throw RequestError("团队简介显示长度>512")
        val team = Team(
            name = name,
            info = info,
            members = listOf(
                Team.Member(
                    id = uid,
                    role = Team.Role.OWNER
                )
            )
        )
        dbcl.insertOne(team)
    }
    suspend fun delete(uid: ObjectId){
        val team = getOwnTeam(uid) ?: throw RequestError("无可删除的团队")
        dbcl.deleteOne(eq("_id",team._id))
    }
    suspend fun invite(uid: ObjectId,targetQQ: String){
        val team = getOwnTeam(uid) ?: throw RequestError("无权限")
        val target = PlayerService.getByQQ(targetQQ) ?: throw RequestError("无此账号")
        if(hasJoinedTeam(target._id)) throw RequestError("目标已在团队中")

    }
}