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
    get("/") {
        TeamService.getJoinedTeam(uid)
            ?.let { response(data = it) }
            ?: throw RequestError("无团队")
    }
    post("/") {
        TeamService.create(
            uid,
            paramNull("name"),
            paramNull("info")
        )
        ok()
    }

    delete("/") {
        install(TeamGuardPlugin) {
            permission = TeamPermission.OWNER_ONLY
        }
        handle {
            val ctx = call.teamGuardContext()
            TeamService.delete(ctx.team, ctx.requester.id)
            ok()
        }
    }

    post("/member/{qq}") {
        install(TeamGuardPlugin) {
            permission = TeamPermission.ADMIN_OR_OWNER
        }
        handle {
            val ctx = call.teamGuardContext()
            TeamService.invite(ctx.team, param("qq"))
            ok()
        }
    }

    delete("/member/{uid2}") {
        install(TeamGuardPlugin) {
            permission = TeamPermission.ADMIN_KICK_MEMBER
            targetIdExtractor = { idParam("uid2") }
            requireTargetMember = true
        }
        handle {
            val ctx = call.teamGuardContext()
            val target = ctx.target ?: throw RequestError("对方不在团队内")
            TeamService.kick(ctx.team, ctx.requester, target)
            ok()
        }
    }

    post("/transfer/{uid2}") {
        install(TeamGuardPlugin) {
            permission = TeamPermission.OWNER_ONLY
            targetIdExtractor = { (idParam("uid2")) }
            requireTargetMember = true
        }
        handle {
            val ctx = call.teamGuardContext()
            val target = ctx.target ?: throw RequestError("对方不在团队内")
            TeamService.transferOwnership(ctx.team, ctx.requester, target)
            ok()
        }
    }

    put("/role/{uid2}/{role}") {
        install(TeamGuardPlugin) {
            permission = TeamPermission.OWNER_ONLY
            targetIdExtractor = { (idParam("uid2")) }
            requireTargetMember = true
        }
        handle {
            val ctx = call.teamGuardContext()
            val target = ctx.target ?: throw RequestError("对方不在团队内")
            val role = Team.Role.valueOf(param("role"))
            TeamService.setRole(ctx.team, ctx.requester, target, role)
            ok()
        }
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
    suspend fun create(uid: ObjectId, name: String?, info: String?) {
        val account = PlayerService.getById(uid) ?: throw RequestError("无此账号")
        val info = info ?: "无"
        val name = name ?: "${account.name}的团队"
        if (hasJoinedTeam(uid)) throw RequestError("已有团队")
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

    suspend fun delete(team: Team, requesterId: ObjectId) {
        team.hostIds.forEach { HostService.delete(team, it) }
        team.worldIds.forEach { WorldService.delete(requesterId, it) }
        dbcl.deleteOne(eq("_id", team._id))
    }

    suspend fun invite(team: Team, targetQQ: String) {
        val target = PlayerService.getByQQ(targetQQ) ?: throw RequestError("无此账号")
        if (hasJoinedTeam(target._id)) throw RequestError("对方已有团队")
        if (team.hasMember(target._id)) throw RequestError("对方已在团队内")
        dbcl.updateOne(
            eq("_id", team._id),
            Updates.push("members", Team.Member(target._id, Team.Role.MEMBER))
        )
    }

    suspend fun kick(team: Team, requester: Team.Member, target: Team.Member) {
        if (requester.id == target.id) throw RequestError("不能踢自己")
        if (requester.role == Team.Role.ADMIN && target.role.level <= Team.Role.ADMIN.level) {
            throw RequestError("管理员不能踢管理员")
        }
        dbcl.updateOne(
            eq("_id", team._id),
            Updates.pull("members", eq("id", target.id))
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

    suspend fun transferOwnership(team: Team, requester: Team.Member, target: Team.Member) {
        if (requester.id == target.id) throw RequestError("不能转给自己")
        dbcl.updateOne(
            eq("_id", team._id),
            Updates.combine(
                Updates.set("members.$[elem1].role", Team.Role.ADMIN),
                Updates.set("members.$[elem2].role", Team.Role.OWNER),
            ),
            UpdateOptions().arrayFilters(
                listOf(
                    org.bson.Document("elem1.id", target.id),
                    org.bson.Document("elem2.id", requester.id)
                )
            )
        )
    }

    suspend fun setRole(team: Team, requester: Team.Member, target: Team.Member, role: Team.Role) {
        if (requester.id == target.id) throw RequestError("不能修改自己")
        if (target.role == Team.Role.OWNER) throw RequestError("不能修改队长身份")
        dbcl.updateOne(
            eq("_id", team._id),
            Updates.set("members.$[elem].role", role),
            UpdateOptions().arrayFilters(
                listOf(
                    org.bson.Document("elem.id", target.id)
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

    suspend fun Team.delWorld(worldId: ObjectId) {
        dbcl.updateOne(
            eq("_id", _id),
            Updates.pull("worldIds", worldId)
        )
    }
}