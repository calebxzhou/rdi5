package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.DB
import calebxzhou.rdi.ihq.exception.ParamError
import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.model.pack.Modpack
import calebxzhou.rdi.ihq.net.*
import calebxzhou.rdi.ihq.service.ModpackService.createVersion
import calebxzhou.rdi.ihq.service.ModpackService.deleteModpack
import calebxzhou.rdi.ihq.service.ModpackService.deleteVersion
import calebxzhou.rdi.ihq.service.ModpackService.getVersionFile
import calebxzhou.rdi.ihq.service.ModpackService.getVersionFileList
import calebxzhou.rdi.ihq.service.ModpackService.uploadVersionFile
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.types.ObjectId


fun Route.modpackRoutes() {
    route("/modpack") {
        post("/") {
            response(data= ModpackService.create(call.uid, param("name")))

        }

        get("/my") {
            val mods = ModpackService.listByAuthor(call.uid)
            response(data = mods)
        }

        get("/{modpackId}") {
            val modpack = ModpackService.get(call.idParam("modpackId"))
                ?: throw RequestError("整合包不存在")
            response(data = modpack)
        }

        delete("/{modpackId}") {
            install(ModpackGuardPlugin)
            handle {
                val ctx = call.modpackGuardContext()
                ctx.deleteModpack()
                ok()
            }
        }

        route("/{modpackId}/{verName}") {
            get("/files") {
                install(ModpackGuardPlugin)
                handle {
                    val ctx = call.modpackGuardContext()
                    val verName = param("verName").trim()
                    val files = ctx.getVersionFileList(verName)
                    response(data = files)
                }
            }

            route("/file/{filePath...}") {
                get {
                    install(ModpackGuardPlugin) 
                    handle {
                        val ctx = call.modpackGuardContext()
                        val verName = param("verName").trim()
                        val filePath = call.parameters.getAll("filePath")?.joinToString("/")
                            ?: throw ParamError("缺少参数: filePath")
                        val content = ctx.getVersionFile(verName, filePath)
                        call.respondBytes(content)
                    }
                }

                post {
                    install(ModpackGuardPlugin) 
                    handle {
                        val ctx = call.modpackGuardContext()
                        val verName = param("verName").trim()
                        val filePath = call.parameters.getAll("filePath")?.joinToString("/")
                            ?: throw ParamError("缺少参数: filePath")
                        val bytes = call.receive<ByteArray>()
                        ctx.uploadVersionFile(verName, filePath, bytes)
                        ok()
                    }
                }
            }

            post {
                install(ModpackGuardPlugin) 
                handle {
                    val ctx = call.modpackGuardContext()
                    val verName = param("verName").trim()
                    ctx.createVersion(verName)
                    ok()
                }
            }

            delete {
                install(ModpackGuardPlugin) 
                handle {
                    val ctx = call.modpackGuardContext()
                    val verName = param("verName").trim()
                    ctx.deleteVersion(verName)
                    ok()
                }
            }
        }
    }
}

object ModpackService {
    private const val MAX_MODPACK_PER_USER = 5
    val dbcl = DB.getCollection<Modpack>("modpack")

    suspend fun listByAuthor(uid: ObjectId): List<Modpack> = dbcl.find(eq("authorId", uid)).toList()

    suspend fun get(id: ObjectId): Modpack? = dbcl.find(eq("_id", id)).firstOrNull()
    suspend fun create(uid: ObjectId, name: String): Modpack {
        val modpackCount = dbcl.countDocuments(eq("authorId", uid)).toInt()
        if (modpackCount >= MAX_MODPACK_PER_USER) {
            throw RequestError("最多5个整合包")
        }
        if (dbcl.countDocuments(eq(Modpack::name.name, name)) > 0) {
            throw RequestError("已存在同名整合包 可以玩别人传过的")
        }
        val player = PlayerService.getById(uid) ?: throw RequestError("无此用户")
        val modPack = Modpack(
            name = player.name + "的整合包${modpackCount + 1}",
            authorId = uid,
        )
        dbcl.insertOne(modPack)
        if (!modPack.dir.exists()) {
            modPack.dir.mkdirs()
        }
        return modPack
    }

    suspend fun ModpackGuardContext.getVersions(): List<Modpack.Version> {
        requireAuthor()
        return reloadModpack().versions
    }

    suspend fun ModpackGuardContext.getVersionFile(verName: String, filePath: String): ByteArray {
        requireAuthor()
        if (filePath.isBlank()) throw RequestError("文件路径不能为空")
        val (pack, _) = requireVersion(verName)
        val versionDir = pack.dir.resolve(verName)
        if (!versionDir.exists() || !versionDir.isDirectory) {
            throw RequestError("版本目录不存在")
        }

        val target = versionDir.resolve(filePath)
        val normalized = target.canonicalFile
        if (!normalized.path.startsWith(versionDir.canonicalPath)) {
            throw RequestError("非法路径")
        }
        if (!normalized.exists() || !normalized.isFile) {
            throw RequestError("文件不存在")
        }

        return normalized.readBytes()
    }

    suspend fun ModpackGuardContext.getVersionFileList(verName: String): List<String> {
        requireAuthor()
        val (pack, _) = requireVersion(verName)
        val dir = pack.dir.resolve(verName)
        if (!dir.exists() || !dir.isDirectory) {
            throw RequestError("版本目录不存在")
        }
        return dir.walkTopDown()
            .filter { it.isFile }
            .map { it.relativeTo(dir).invariantSeparatorsPath }
            .toList()
    }

    suspend fun ModpackGuardContext.createVersion(verName: String) {
        requireAuthor()
        val name = verName.trim()
        if (name.isEmpty()) throw RequestError("版本名不能为空")
        val pack = reloadModpack()
        if (pack.versions.any { it.name == name }) {
            throw RequestError("版本已存在")
        }
        val version = Modpack.Version(
            modpackId = pack._id,
            name = name,
            changelog = "",
            mods = emptyList()
        )
        dbcl.updateOne(
            eq("_id", pack._id),
            Updates.push(Modpack::versions.name, version)
        )
        val dir = pack.dir.resolve(name)
        if (!dir.exists() && !dir.mkdirs()) {
            throw RequestError("创建版本目录失败")
        }
    }

    suspend fun ModpackGuardContext.uploadVersionFile(verName: String, path: String, content: ByteArray) {
        requireAuthor()
        if (path.isBlank()) throw RequestError("文件路径不能为空")
        val (pack, _) = requireVersion(verName)
        val versionDir = pack.dir.resolve(verName)
        if (!versionDir.exists() && !versionDir.mkdirs()) {
            throw RequestError("无法创建版本目录")
        }

        val target = versionDir.resolve(path)
        val normalized = target.canonicalFile
        if (!normalized.path.startsWith(versionDir.canonicalPath)) {
            throw RequestError("非法路径")
        }

        normalized.parentFile?.mkdirs()
        normalized.writeBytes(content)
    }

    suspend fun ModpackGuardContext.deleteVersion(verName: String) {
        requireAuthor()
        val (pack, _) = requireVersion(verName)
        val updated = pack.versions.filterNot { it.name == verName }
        dbcl.updateOne(
            eq("_id", pack._id),
            Updates.set(Modpack::versions.name, updated)
        )
        val dir = pack.dir.resolve(verName)
        if (dir.exists()) {
            dir.deleteRecursively()
        }
    }

    suspend fun ModpackGuardContext.deleteModpack() {
        requireAuthor()
        val pack = reloadModpack()
        val result = dbcl.deleteOne(eq("_id", pack._id))
        if (result.deletedCount == 0L) {
            throw RequestError("整合包不存在")
        }
        val dir = pack.dir
        if (dir.exists()) {
            dir.deleteRecursively()
        }
    }

    suspend fun deleteModpack(uid: ObjectId, modpackId: ObjectId) {
        val pack = get(modpackId) ?: throw RequestError("整合包不存在")
        if (pack.authorId != uid) {
            throw RequestError("不是你的整合包")
        }
        val result = dbcl.deleteOne(eq("_id", modpackId))
        if (result.deletedCount == 0L) {
            throw RequestError("整合包不存在")
        }
        if (pack.dir.exists()) {
            pack.dir.deleteRecursively()
        }
    }

    private suspend fun ModpackGuardContext.reloadModpack(): Modpack =
        get(modpack._id) ?: throw RequestError("整合包不存在")

    private suspend fun ModpackGuardContext.requireVersion(verName: String): Pair<Modpack, Modpack.Version> {
        val pack = reloadModpack()
        val version = pack.versions.firstOrNull { it.name == verName }
            ?: throw RequestError("版本不存在")
        return pack to version
    }

    suspend fun update(uid: ObjectId, modpackId: ObjectId, version: Modpack.Version) {
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