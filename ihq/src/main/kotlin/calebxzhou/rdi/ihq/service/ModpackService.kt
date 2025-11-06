package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.DB
import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.model.pack.ModPack
import calebxzhou.rdi.ihq.net.err
import calebxzhou.rdi.ihq.net.ok
import calebxzhou.rdi.ihq.net.param
import calebxzhou.rdi.ihq.net.response
import calebxzhou.rdi.ihq.net.uid
import com.mongodb.client.model.Filters.and
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import com.mongodb.client.model.Filters.eq
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId


fun Route.modpackRoutes() {
    post("/create") {
        val version = call.receive<ModPack.CreateDto>()
        ModpackService.create(call.uid, version)
        ok()
    }
    post("/update/{modpackId}") {
        ModpackService.update(call.uid, ObjectId(param("modpackId")),call.receive())
        ok()
    }
    get("/{modpackId}/{verName}/mods"){
        ModpackService.vdbcl.find(
        and(
            eq("name", param("verName")),
            eq("_id", param("modpackId"))
        )
        ).firstOrNull()?.mods?.let {
            response(data=it)
        }?:response(data=listOf<Any>())
    }
    get("/my") {
        val mods = ModpackService.pdbcl.find(eq("authorId", call.uid)).toList()
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
    private const val MAX_VERSION_PER_MODPACK = 128
    val pdbcl = DB.getCollection<ModPack>("modpack")
    val vdbcl = DB.getCollection<ModPack.Version>("modpack_version")

    suspend fun create(uid: ObjectId, dto: ModPack.CreateDto) {
        val modpackCount = pdbcl.countDocuments(eq("authorId", uid))
        if (modpackCount >= MAX_MODPACK_PER_USER.toLong()) {
            throw RequestError("最多5个整合包")
        }
        val verId = ObjectId()
        val modPack = ModPack(
            name = dto.name,
            authorId = uid,
            versions = listOf(verId)
        )
        val version = ModPack.Version(
            _id = verId,
            modpackId = modPack._id,
            name = "1.0",
            changelog = "初次发布",
            mods = dto.mods,
            configs = dto.configs,
            kjs = dto.kjs
        )
        //todo build image: 从cf下载mod ， cache有 不用下
        vdbcl.insertOne(version)
        pdbcl.insertOne(modPack)
    }

    suspend fun deleteVersion(uid: ObjectId, modpackId: ObjectId, versionId: ObjectId) {
        val modpack = pdbcl.find(eq("_id", modpackId)).firstOrNull() ?: throw RequestError("整合包不存在")
        if (modpack.authorId != uid) {
            throw RequestError("不是你的包")
        }
        if (!modpack.versions.contains(versionId)) {
            throw RequestError("版本不存在")
        }
        if (modpack.versions.size <= 1) {
            throw RequestError("至少保留一个版本")
        }
        vdbcl.deleteOne(eq("_id", versionId))
        pdbcl.updateOne(eq("_id", modpackId), com.mongodb.client.model.Updates.pull("versions", versionId))
    }

    suspend fun deleteModpack(uid: ObjectId, modpackId: ObjectId) {
        val modpack = pdbcl.find(eq("_id", modpackId)).firstOrNull() ?: throw RequestError("整合包不存在")
        if (modpack.authorId != uid) {
            throw RequestError("不是你的包")
        }
        vdbcl.deleteMany(eq("modpackId", modpackId))
        pdbcl.deleteOne(eq("_id", modpackId))
    }
    suspend fun update(uid: ObjectId,modpackId: ObjectId,version: ModPack.Version){
        val modpack = pdbcl.find(eq("_id", modpackId)).firstOrNull() ?: throw RequestError("整合包不存在")
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
        vdbcl.insertOne(versionToInsert)
        pdbcl.updateOne(eq("_id", modpackId), com.mongodb.client.model.Updates.push("versions", newVersionId))
    }
}