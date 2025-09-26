package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.DB
import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.model.Team
import calebxzhou.rdi.ihq.net.*
import calebxzhou.rdi.ihq.util.displayLength
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
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
    get("/my") {
        TeamService.getJoinedTeam(uid)
            ?.let { response(data = it) }
            ?: throw RequestError("无团队")
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
        TeamService.invite(uid, param("qq"))
        ok()
    }
    post("/kick") {
        TeamService.kick(uid, ObjectId(param("uid2")))
        ok()
    }
    post("/transfer") {
        TeamService.transferOwnership(uid, ObjectId(param("uid2")))
        ok()
    }
    post("/role") {
        TeamService.setRole(uid, ObjectId(param("uid2")), Team.Role.valueOf(param("role")))
        ok()
    }

}

object TeamService {
    val dbcl = DB.getCollection<Team>("team")

    init {
        runBlocking {
            HostService.dbcl.createIndex(
                Indexes.ascending("members.id"),
            )
        }
    }

    //玩家拥有的团队
    suspend fun getOwn(uid: ObjectId): Team? = dbcl.find(
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
    suspend fun hasOwnTeam(uid: ObjectId): Boolean =
        dbcl.find(and(eq("members.id", uid), eq("members.role", Team.Role.OWNER))).firstOrNull() != null

    suspend fun get(id: ObjectId) = dbcl.find(eq("_id", id)).firstOrNull()
    suspend fun has(id: ObjectId) = get(id) != null
    suspend fun create(uid: ObjectId, name: String, info: String) {
        if (hasJoinedTeam(uid)) throw RequestError("已有团队")
        if (!PlayerService.has(uid)) throw RequestError("无此账号")
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

    suspend fun delete(uid: ObjectId) {
        val team = getOwn(uid) ?: throw RequestError("无团队")
        dbcl.deleteOne(eq("_id", team._id))
    }

    suspend fun invite(uid: ObjectId, targetQQ: String) {
        val team = getOwn(uid) ?: throw RequestError("无团队")
        val target = PlayerService.getByQQ(targetQQ) ?: throw RequestError("无此账号")
        if (hasJoinedTeam(target._id)) throw RequestError("对方已有团队")
        HostService.dbcl.updateOne(
            eq("_id", team._id),
            Updates.push("members", Team.Member(target._id, Team.Role.MEMBER))
        )
    }

    suspend fun kick(uid: ObjectId, uid2: ObjectId) {
        if (uid == uid2) throw RequestError("不能踢自己")
        val team = getOwn(uid) ?: throw RequestError("无团队")
        if (!PlayerService.has(uid2)) throw RequestError("无此账号")
        if (!team.hasMember(uid2)) throw RequestError("对方不在团队内")
        dbcl.updateOne(
            eq("_id", team._id),
            Updates.pull("members", eq("id", uid2))
        )

    }

    suspend fun quit(uid: ObjectId) {
        val team = getJoinedTeam(uid) ?: throw RequestError("无团队")
        if (team.owner?.id == uid) throw RequestError("你是队长，只能解散团队")
        dbcl.updateOne(
            eq("_id", team._id),
            Updates.pull("members", eq("id", uid))
        )
    }

    suspend fun transferOwnership(uid: ObjectId, uid2: ObjectId) {
        if (uid == uid2) throw RequestError("不能转给自己")
        val team = getOwn(uid) ?: throw RequestError("你无团队")
        if (!PlayerService.has(uid2)) throw RequestError("无此账号")
        if (!team.hasMember(uid2)) throw RequestError("对方不在团队内")
        dbcl.updateOne(
            eq("_id", team._id),
            Updates.combine(
                Updates.set("members.$[elem1].role", Team.Role.ADMIN),
                Updates.set("members.$[elem2].role", Team.Role.OWNER),
            ),
            UpdateOptions().arrayFilters(
                listOf(
                    org.bson.Document(
                        "arrayFilters", listOf(
                            org.bson.Document("elem1.id", uid2),
                            org.bson.Document("elem2.id", uid),
                        )
                    )
                )
            )
        )
    }

    suspend fun setRole(uid: ObjectId, uid2: ObjectId, role: Team.Role) {
        if (uid == uid2) throw RequestError("不能修改自己")
        val team = getOwn(uid) ?: throw RequestError("你无团队")
        if (!PlayerService.has(uid2)) throw RequestError("无此账号")
        if (!team.hasMember(uid2)) throw RequestError("对方不在团队内")
        dbcl.updateOne(
            eq("_id", team._id),
            Updates.set("members.$[elem].role", role),
            UpdateOptions().arrayFilters(
                listOf(
                    org.bson.Document(
                        "arrayFilters", listOf(
                            org.bson.Document("elem.id", uid2),
                        )
                    )
                )
            )
        )
    }

    suspend fun Team.addHost(hostId: ObjectId) {
        dbcl.updateOne(
            eq("_id", _id),
            Updates.push("hostIds", hostId)
        )
    }

    suspend fun Team.delHost(hostId: ObjectId) {
        dbcl.updateOne(
            eq("_id", _id),
            Updates.pull("hostIds", hostId)
        )
    }

    suspend fun Team.addWorld(worldId: ObjectId) {
        dbcl.updateOne(
            eq("_id", _id),
            Updates.push("worldIds", worldId)
        )
    }
}