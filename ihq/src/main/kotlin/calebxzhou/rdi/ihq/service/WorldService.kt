package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.DB
import calebxzhou.rdi.ihq.DEFAULT_MODPACK_ID
import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.lgr
import calebxzhou.rdi.ihq.model.World
import calebxzhou.rdi.ihq.net.ok
import calebxzhou.rdi.ihq.net.param
import calebxzhou.rdi.ihq.net.paramNull
import calebxzhou.rdi.ihq.net.response
import calebxzhou.rdi.ihq.net.uid
import calebxzhou.rdi.ihq.util.displayLength
import calebxzhou.rdi.ihq.service.WorldService.createWorld
import com.mongodb.client.model.Filters.eq
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId

fun Route.worldRoutes() = route("/world") {
    get {
        response(data = WorldService.listByOwner(uid))
    }
    post {
        val modpackId = ObjectId(param("modpackId"))
        createWorld(uid,paramNull("name"), modpackId)
    }
    route("/{worldId}") {
        post("/copy") {
            val sourceId = ObjectId(param("worldId"))
            val name = paramNull("name")
            val world = WorldService.duplicate(uid, sourceId, name)
            response(data = world)
        }
        delete {
            val worldId = ObjectId(param("worldId"))
            WorldService.delete(uid, worldId)
            ok()
        }
    }


}

object WorldService {
    private const val PLAYER_MAX_WORLD = 5

    val dbcl = DB.getCollection<World>("world")

    suspend fun getById(id: ObjectId): World? = dbcl.find(eq("_id", id)).firstOrNull()

    suspend fun listByOwner(ownerId: ObjectId): List<World> =
        dbcl.find(eq("ownerId", ownerId)).toList()


    private suspend fun ensureCapacity(teamId: ObjectId) {
        if (listByOwner(teamId).size >= PLAYER_MAX_WORLD) {
            throw RequestError("存档最多${PLAYER_MAX_WORLD}个")
        }
    }

    suspend fun createWorld(uid: ObjectId,name: String?, packId: ObjectId): World {
        val name = name ?: "存档${listByOwner(uid).size + 1}"
        if (name.displayLength > 32) throw RequestError("名称过长")
        ensureCapacity(uid)
        val world = World(name = name, ownerId = uid, modpackId = packId)
        try {
            DockerService.createVolume(world._id.toHexString())
        } catch (e: Exception) {
            lgr.error(e) { "{}" }
            throw RequestError("创建存档失败:${e.message}")
        }
        dbcl.insertOne(world) 
        return world
    }

    suspend fun duplicate(uid: ObjectId, worldId: ObjectId, newName: String?): World {
        val world = getById(worldId) ?: throw RequestError("存档不存在")
        val newName = newName ?: (world.name+"副本")
        ensureCapacity(uid)
        if (newName.displayLength > 64) throw RequestError("名称过长")
        if (world.ownerId != uid) throw RequestError("无权限")
        val newWorld = World(name = newName, ownerId = uid, modpackId = world.modpackId)
        try {
            DockerService.cloneVolume(world._id.toHexString(), newWorld._id.toHexString())
        } catch (e: Exception) {
            DockerService.deleteVolume(newWorld._id.toHexString())
            throw e
        }
        dbcl.insertOne(newWorld)
        return newWorld
    }

    suspend fun delete(uid: ObjectId, worldId: ObjectId) {
        val world = getById(worldId) ?: throw RequestError("存档不存在")
        if(world.ownerId != uid) throw RequestError("无权限")
        HostService.findByWorld(worldId)?.let { throw RequestError("存档已被放入主机“${it.name}”中使用，要删除存档，先关闭主机并拔出存档") }
        dbcl.deleteOne(eq("_id", worldId))
        DockerService.deleteVolume(world._id.toHexString())
    }

    /*suspend fun mount(uid: ObjectId, hostId: ObjectId, worldId: ObjectId) {
        val host = HostService.getById(hostId) ?: throw RequestError("无此主机")
        val world = getById(worldId) ?: throw RequestError("存档不存在")
        if (host.teamId != world.teamId) throw RequestError("主机与存档不属于同一团队")
        val team = TeamService.get(world.teamId) ?: throw RequestError("无此团队")
        if (!team.isOwnerOrAdmin(uid)) throw RequestError("无权限")
        HostService.findByWorld(worldId)?.let {
            if (it._id != hostId) throw RequestError("存档已被主机占用")
        }
        HostService.remountWorld(host, worldId)
    }

    suspend fun unmount(uid: ObjectId, hostId: ObjectId) {
        val host = HostService.getById(hostId) ?: throw RequestError("无此主机")
        val team = TeamService.get(host.teamId) ?: throw RequestError("无此团队")
        if (!team.isOwnerOrAdmin(uid)) throw RequestError("无权限")
        HostService.remountWorld(host, null)
    }*/
}
