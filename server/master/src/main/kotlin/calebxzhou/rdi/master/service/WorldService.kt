package calebxzhou.rdi.master.service

import calebxzhou.mykotutils.std.displayLength
import calebxzhou.rdi.master.DB
import calebxzhou.rdi.common.model.World
import calebxzhou.rdi.common.model.RAccount
import calebxzhou.rdi.master.net.*
import calebxzhou.rdi.master.service.WorldService.createWorld
import calebxzhou.mykotutils.log.Loggers
import calebxzhou.mykotutils.std.deleteRecursivelyNoSymlink
import calebxzhou.rdi.common.exception.RequestError
import calebxzhou.rdi.common.model.isDav
import calebxzhou.rdi.common.util.validateName
import calebxzhou.rdi.master.WORLDS_DIR
import calebxzhou.rdi.master.service.WorldService.toVo
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates.set
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId
import java.nio.file.Files

fun Route.worldRoutes() = route("/world") {
    get {
        response(data = WorldService.listByOwnerVo(uid))
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
            response(data = world.toVo())
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
    private val lgr by Loggers
    val dbcl = DB.getCollection<World>("world")
    fun getDir(worldId: ObjectId) = WORLDS_DIR.resolve(worldId.toHexString())
    fun getDataDir(worldId: ObjectId) = getDir(worldId).resolve("data").also { it.mkdir() }
    val World.dir get() = getDir(_id)
    val World.dataDir get() = getDataDir(_id)
    suspend fun getById(id: ObjectId): World? = dbcl.find(eq("_id", id)).firstOrNull()

    suspend fun listByOwner(ownerId: ObjectId): List<World> =
        dbcl.find(eq("ownerId", ownerId)).toList()

    suspend fun listByOwnerVo(ownerId: ObjectId): List<World.Vo> =
        listByOwner(ownerId).map { it.toVo() }

    suspend fun World.toVo(): World.Vo {
        val modpack = ModpackService.getById(modpackId)
        return World.Vo(
            id = _id,
            name = name,
            ownerId = ownerId,
            size = size,
            modpackId = modpackId,
            modpackName = modpack?.name ?: "未知整合包",
            modpackIconUrl = modpack?.iconUrl
        )
    }

    suspend fun updateWorldSize(worldId: ObjectId): Long {
        val worldDir = getDir(worldId)
        val totalSize = if (worldDir.exists()) {
            worldDir.walkTopDown()
                .filter { it.isFile && !Files.isSymbolicLink(it.toPath()) }
                .sumOf { it.length() }
        } else {
            0L
        }
        dbcl.updateOne(eq("_id", worldId), set(World::size.name, totalSize))
        return totalSize
    }

    suspend fun RAccount.ownWorlds() = WorldService.listByOwner(_id)

    private suspend fun ensureCapacity(ownerId: ObjectId) {
        val player = PlayerService.getById(ownerId)
        if (listByOwner(ownerId).size >= PLAYER_MAX_WORLD && player?.isDav == false) {
            throw RequestError("存档最多${PLAYER_MAX_WORLD}个")
        }
    }

    suspend fun createWorld(uid: ObjectId,name: String?, packId: ObjectId): World {
        val modpack = ModpackService.getById(packId)?:throw RequestError("整合包不存在")
        val name = name ?: "存档${listByOwner(uid).size + 1}"
        name.validateName()
        ensureCapacity(uid)
        val world = World(name = name, ownerId = uid, modpackId = packId)
        world.dir.mkdir()
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
        val sourceDir = world.dir
        if (!sourceDir.exists() || !sourceDir.isDirectory) {
            throw RequestError("存档不存在或创建失败")
        }
        newWorld.dir.mkdirs()
        sourceDir.copyRecursively(newWorld.dir, overwrite = true)
        dbcl.insertOne(newWorld)
        return newWorld
    }

    suspend fun delete(uid: ObjectId, worldId: ObjectId) {
        val world = getById(worldId) ?: throw RequestError("存档不存在")
        if(world.ownerId != uid) throw RequestError("无权限")
        HostService.findByWorld(worldId)?.let { throw RequestError("须先删除地图“${it.name}”，再删除此区块数据") }
        dbcl.deleteOne(eq("_id", worldId))
        val dir = world.dir
        if (dir.exists()) {
            dir.deleteRecursivelyNoSymlink()
        }
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
