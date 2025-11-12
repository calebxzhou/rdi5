package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.DB
import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.model.Team
import calebxzhou.rdi.ihq.model.pack.Mod
import calebxzhou.rdi.ihq.model.pack.ModPack
import calebxzhou.rdi.ihq.net.ok
import calebxzhou.rdi.ihq.net.param
import calebxzhou.rdi.ihq.net.response
import calebxzhou.rdi.ihq.net.uid
import com.mongodb.client.model.Filters.and
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId
import javax.ws.rs.sse.SseEventSource.target


fun Route.modpackRoutes() {
    post("/create") {
        ModpackService.create(call.uid, param("name"))
        ok()
    }
    post("/update/{modpackId}") {
        ModpackService.update(call.uid, ObjectId(param("modpackId")), call.receive())
        ok()
    }
    /*get("/{modpackId}/{verName}/mods"){
        ModpackService.vdbcl.find(
        and(
            eq("name", param("verName")),
            eq("_id", param("modpackId"))
        )
        ).firstOrNull()?.mods?.let {
            response(data=it)
        }?:response(data=listOf<Any>())
    }*/
    route("/{modpackId}/{verName}") {
        get { }
        post {

        }
        delete { }
    }
    get("/my") {
        val mods = ModpackService.dbcl.find(eq("authorId", call.uid)).toList()
        response(data = mods)
    }
    delete("/{modpackId}") {
        ModpackService.deleteModpack(call.uid, ObjectId(param("modpackId")))
        ok()
    }
    delete("/{modpackId}/{versionId}") {
        ModpackService.deleteVersion(call.uid, ObjectId(param("modpackId")), ObjectId(param("versionId")))
        ok()
    }
}

object ModpackService {
    private const val MAX_MODPACK_PER_USER = 5
    val dbcl = DB.getCollection<ModPack>("modpack")
    suspend fun create(uid: ObjectId, name: String) {
        val modpackCount = dbcl.countDocuments(eq("authorId", uid)).toInt()
        if (modpackCount >= MAX_MODPACK_PER_USER) {
            throw RequestError("最多5个整合包")
        }
        if (dbcl.countDocuments(eq(ModPack::name.name, name)) > 0) {
            throw RequestError("已存在同名整合包 可以玩别人传过的")
        }
        val player = PlayerService.getById(uid) ?: throw RequestError("无此用户")
        val modPack = ModPack(
            name = player.name + "的整合包${modpackCount + 1}",
            authorId = uid,
        )
        dbcl.insertOne(modPack)
        modPack.dir.mkdir()
    }

    suspend fun ModpackGuardContext.createVersion(verName: String) {
        modpack.dir.resolve(verName).mkdir()
        dbcl.updateOne(
            eq("_id", modpack._id),
            Updates.push(ModPack::versions.name, verName)
        )
    }
    suspend fun uploadVersionFile(uid: ObjectId, modpackId: ObjectId,verName: String){

    }

    suspend fun deleteVersion(uid: ObjectId, modpackId: ObjectId, versionId: ObjectId) {
        val modpack = dbcl.find(eq("_id", modpackId)).firstOrNull() ?: throw RequestError("整合包不存在")
        /* if (modpack.authorId != uid) {
             throw RequestError("不是你的包")
         }
         if (!modpack.versions.contains(versionId)) {
             throw RequestError("版本不存在")
         }
         if (modpack.versions.size <= 1) {
             throw RequestError("至少保留一个版本")
         }
         vdbcl.deleteOne(eq("_id", versionId))
         dbcl.updateOne(eq("_id", modpackId), com.mongodb.client.model.Updates.pull("versions", versionId))
        */
    }

    suspend fun deleteModpack(uid: ObjectId, modpackId: ObjectId) {
        /* val modpack = dbcl.find(eq("_id", modpackId)).firstOrNull() ?: throw RequestError("整合包不存在")
         if (modpack.authorId != uid) {
             throw RequestError("不是你的包")
         }
         vdbcl.deleteMany(eq("modpackId", modpackId))
         dbcl.deleteOne(eq("_id", modpackId))
        */
    }

    suspend fun update(uid: ObjectId, modpackId: ObjectId, version: ModPack.Version) {
        /* val modpack = dbcl.find(eq("_id", modpackId)).firstOrNull() ?: throw RequestError("整合包不存在")
         if (modpack.authorId != uid) {
             throw RequestError("不是你的包")
         }
         if (modpack.versions.size >= MAX_VERSION_PER_MODPACK) {
             throw RequestError("版本最多${MAX_VERSION_PER_MODPACK}个")
         }
         val newVersionId = ObjectId()
         val versionToInsert = version.copy(
             _id = newVersionId,
             modpackId = modpackId
         )
         dbcl.updateOne(eq("_id", modpackId), com.mongodb.client.model.Updates.push("versions", newVersionId))
        */
    }
}