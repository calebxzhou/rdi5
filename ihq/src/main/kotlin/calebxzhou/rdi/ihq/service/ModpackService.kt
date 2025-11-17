package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.BASE_IMAGE_DIR
import calebxzhou.rdi.ihq.DB
import calebxzhou.rdi.ihq.exception.ParamError
import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.lgr
import calebxzhou.rdi.ihq.model.pack.Mod
import calebxzhou.rdi.ihq.model.pack.Modpack
import calebxzhou.rdi.ihq.model.pack.ModpackDetailedVo
import calebxzhou.rdi.ihq.model.pack.ModpackVo
import calebxzhou.rdi.ihq.net.*
import calebxzhou.rdi.ihq.service.ModpackService.buildAsImage
import calebxzhou.rdi.ihq.service.ModpackService.createVersion
import calebxzhou.rdi.ihq.service.ModpackService.deleteModpack
import calebxzhou.rdi.ihq.service.ModpackService.deleteVersion
import calebxzhou.rdi.ihq.service.ModpackService.getVersion
import calebxzhou.rdi.ihq.service.ModpackService.getVersionFile
import calebxzhou.rdi.ihq.service.ModpackService.getVersionFileList
import calebxzhou.rdi.ihq.service.ModpackService.requireVersion
import calebxzhou.rdi.ihq.service.ModpackService.toDetailVo
import calebxzhou.rdi.ihq.service.ModpackService.uploadVersionFile
import calebxzhou.rdi.ihq.service.PlayerService.getPlayerNames
import calebxzhou.rdi.ihq.util.scope
import calebxzhou.rdi.ihq.util.serdesJson
import calebxzhou.rdi.ihq.util.str
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.regex
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import io.ktor.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.io.readByteArray
import org.bson.Document
import org.bson.types.ObjectId
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.zip.ZipInputStream


fun Route.modpackRoutes() {
    route("/modpack") {
        get {
            response(data = ModpackService.listAll())
        }
        post {
            response(data = ModpackService.create(call.uid, param("name")))
        }
        get("/my") {
            val mods = ModpackService.listByAuthor(call.uid)
            response(data = mods)
        }
        route("/{modpackId}") {
            install(ModpackGuardPlugin)
            get {

                response(data = call.modpackGuardContext().modpack.toDetailVo())
            }

            delete {

                val ctx = call.modpackGuardContext()
                ctx.deleteModpack()
                ok()

            }

            route("/version/{verName}") {
                get {
                    val ctx = call.modpackGuardContext()
                    val verName = param("verName").trim()
                    val version = ctx.getVersion(verName)
                    response(data = version)
                }
                delete {
                    val ctx = call.modpackGuardContext()
                    val verName = param("verName").trim()
                    ctx.deleteVersion(verName)
                    ok()

                }
                post("/rebuild") {
                    val ctx = call.modpackGuardContext()
                    val verName = param("verName").trim()
                    ctx.requireVersion(verName).run { first.buildAsImage(second) }
                    ok()
                }
                post {
                    val ctx = call.modpackGuardContext()
                    val verName = param("verName").trim()
                    //limit 1 GB
                    val multipart = call.receiveMultipart(formFieldLimit = 1024 * 1024 * 1024)
                    var zipBytes: ByteArray? = null
                    var mods: List<Mod>? = null

                    while (true) {
                        val part = multipart.readPart() ?: break
                        when (part) {
                            is PartData.FormItem -> if (part.name == "mods") {
                                mods = runCatching { serdesJson.decodeFromString<List<Mod>>(part.value) }
                                    .getOrElse { throw ParamError("mods格式错误: ${it.message}") }
                            }

                            is PartData.FileItem -> if (part.name == "file") {
                                zipBytes = part.provider().toByteArray()
                            }

                            is PartData.BinaryItem -> if (part.name == "file") {
                                zipBytes = part.provider().readByteArray()
                            }

                            else -> {}
                        }
                    }

                    val payload = zipBytes ?: throw ParamError("缺少文件")
                    val modList = mods ?: emptyList()

                    ctx.createVersion(verName, payload, modList)
                    ok()

                }
                get("/files") {

                    val ctx = call.modpackGuardContext()
                    val verName = param("verName").trim()
                    val files = ctx.getVersionFileList(verName)
                    response(data = files)

                }

                get("/file/{filePath...}") {

                    install(ModpackGuardPlugin)

                    val ctx = call.modpackGuardContext()
                    val verName = param("verName").trim()
                    val filePath = call.parameters.getAll("filePath")?.joinToString("/")
                        ?: throw ParamError("缺少参数: filePath")
                    val content = ctx.getVersionFile(verName, filePath)
                    call.respondBytes(content)

                }
                post("/file/{filePath...}") {

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

        get("/search/{modpackName}") {
            val name = call.parameters["modpackName"]?.trim()
                ?: throw ParamError("缺少参数: modpackName")
            val modpacks = ModpackService.searchByName(name)
            response(data = modpacks)
        }


    }
}

object ModpackService {
    private const val MAX_MODPACK_PER_USER = 5
    val dbcl = DB.getCollection<Modpack>("modpack")

    suspend fun listByAuthor(uid: ObjectId): List<Modpack> = dbcl.find(eq("authorId", uid)).toList()

    suspend fun get(id: ObjectId): Modpack? = dbcl.find(eq("_id", id)).firstOrNull()

    suspend fun searchByName(name: String): List<Modpack> {
        if (name.isBlank()) return emptyList()
        //Uses MongoDB's regex filter with case-insensitive flag ("i")
        return dbcl.find(regex(Modpack::name.name, name, "i")).toList()
    }

    suspend fun listAll(): List<ModpackVo> {
        val modpacks = dbcl.find().toList()
        if (modpacks.isEmpty()) return emptyList()

        val authorNames = modpacks.map { it.authorId }.getPlayerNames()

        return modpacks.map { pack ->
            ModpackVo(
                pack._id,
                name = pack.name,
                authorId = pack.authorId,
                authorName = authorNames[pack.authorId] ?: "未知作者",
                modCount = pack.versions.maxOfOrNull { it.mods.size } ?: 0,
                fileSize = pack.totalSize(),
                icon = pack.icon,
                info = pack.info
            )
        }
    }

    suspend fun Modpack.toDetailVo(): ModpackDetailedVo {
        val authorName = PlayerService.getName(authorId) ?: "未知作者"
        return ModpackDetailedVo(
            id = _id,
            name = name,
            authorId = authorId,
            authorName = authorName,
            modCount = versions.maxOfOrNull { it.mods.size } ?: 0,
            fileSize = totalSize(),
            icon = icon,
            info = info,
            modloader = modloader,
            mcVer = mcVer,
            versions = versions
        )
    }

    suspend fun create(uid: ObjectId, name: String): Modpack {
        val modpackCount = dbcl.countDocuments(eq("authorId", uid)).toInt()
        if (modpackCount >= MAX_MODPACK_PER_USER) {
            throw RequestError("最多5个整合包")
        }
        if (dbcl.countDocuments(eq(Modpack::name.name, name)) > 0) {
            throw RequestError("其他人已上传过这个整合包了")
        }
        val player = PlayerService.getById(uid) ?: throw RequestError("无此用户")
        val modPack = Modpack(
            name = name,
            authorId = uid,
        )
        dbcl.insertOne(modPack)
        if (!modPack.dir.exists()) {
            modPack.dir.mkdirs()
        }
        return modPack
    }

    suspend fun ModpackGuardContext.getVersions(): List<Modpack.Version> {
        return reloadModpack().versions
    }

    suspend fun ModpackGuardContext.getVersion(verName: String): Modpack.Version {
        requireAuthor()
        val (_, version) = requireVersion(verName)
        return version
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

    suspend fun ModpackGuardContext.createVersion(verName: String, zipBytes: ByteArray, mods: List<Mod>) {
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
            changelog = "新上传",
            mods = mods, time = System.currentTimeMillis()
        )

        val zipFile = pack.dir.resolve("${name}.zip")
        zipFile.parentFile?.mkdirs()
        zipFile.writeBytes(zipBytes)

        scope.launch {
            lgr.info { "开始处理整合包 ${pack._id}:${version.name}" }
            runCatching {
                pack.finalizeUploadedVersion(version, zipFile)
            }.onSuccess {
                lgr.info { "整合包 ${pack._id}:${version.name} 处理完成" }
            }.onFailure { error ->
                lgr.error(error) { "处理整合包 ${pack._id}:${version.name} 失败" }
            }
        }
    }

    private suspend fun Modpack.finalizeUploadedVersion(version: Modpack.Version, zipFile: java.io.File) {
        val verdir = version.dir
        if (verdir.exists()) {
            verdir.deleteRecursively()
        }
        if (!verdir.mkdirs()) {
            throw RequestError("创建版本目录失败")
        }

        try {
            unzipOverrides(zipFile, verdir)
            val versionSize = verdir.walkTopDown()
                .filter { it.isFile }
                .sumOf { it.length() }
            verdir.resolve("total_size.txt").writeText(versionSize.toString())

            dbcl.updateOne(
                eq("_id", _id),
                Updates.push(Modpack::versions.name, version)
            )

            lgr.info { "开始构建镜像 ${_id}:${version.name}" }
            runCatching { buildAsImage(version) }
                .onFailure { error ->
                    lgr.error(error) { "镜像构建失败 ${_id}:${version.name}" }
                }
        } catch (e: Exception) {
            if (verdir.exists()) {
                verdir.deleteRecursively()
            }
            throw when (e) {
                is RequestError -> e
                else -> RequestError("处理整合包失败: ${e.message}")
            }
        }
    }

    private fun unzipOverrides(zipFile: java.io.File, targetDir: java.io.File) {
        val versionDirPath = targetDir.toPath()
        ZipInputStream(zipFile.inputStream().buffered()).use { zipInput ->
            var entry = zipInput.nextEntry
            while (entry != null) {
                val relativePath = extractOverridesRelativePath(entry.name)
                if (relativePath != null) {
                    val resolvedPath = versionDirPath.resolve(relativePath).normalize()
                    if (!resolvedPath.startsWith(versionDirPath)) {
                        throw RequestError("非法文件路径: ${entry.name}")
                    }

                    if (entry.isDirectory) {
                        Files.createDirectories(resolvedPath)
                    } else {
                        resolvedPath.parent?.let { Files.createDirectories(it) }
                        Files.newOutputStream(
                            resolvedPath,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING
                        ).use { output ->
                            zipInput.copyTo(output)
                        }
                    }
                }
                zipInput.closeEntry()
                entry = zipInput.nextEntry
            }
        }
    }

    suspend fun Modpack.buildAsImage(version: Modpack.Version) {
        val modsDir = version.dir.resolve("mods").apply { mkdirs() }
        BASE_IMAGE_DIR.resolve("${mcVer}_${modloader}").copyRecursively(version.dir, true)
        val downloadedMods = CurseForgeService.downloadMods(version.mods)


        downloadedMods.forEach { sourcePath ->
            val fileName = sourcePath.fileName?.toString() ?: return@forEach
            val targetPath = modsDir.resolve(fileName).toPath()
            targetPath.parent?.let { Files.createDirectories(it) }
            val absoluteSource = sourcePath.toAbsolutePath()
            Files.copy(absoluteSource, targetPath, StandardCopyOption.REPLACE_EXISTING)
        }

        val imageTag = "${_id.str}:${version.name}"
        val imageId = DockerService.buildImage(
            imageTag,
            version.dir,
            onLine = { log -> lgr.info { "build image ${imageTag}: ${log}" } },
            onError = { throwable -> lgr.error(throwable) { "build image ${imageTag} failed" } },
            onFinished = {
                lgr.info { "build image ${imageTag} finished" }
                scope.launch {
                    version.setReady(true)
                }
            }
        )

        lgr.info { "build image $imageTag success: $imageId" }


    }

    suspend fun Modpack.Version.setReady(ready: Boolean) {
        dbcl.updateOne(
            eq(Modpack::_id.name, modpackId),
            Updates.set("${Modpack::versions.name}.$[elem].${Modpack.Version::ready.name}", ready),
            UpdateOptions().arrayFilters(
                listOf(
                    Document("elem.name", name)
                )
            )
        )
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

    suspend fun ModpackGuardContext.reloadModpack(): Modpack =
        get(modpack._id) ?: throw RequestError("整合包不存在")

    suspend fun ModpackGuardContext.requireVersion(verName: String): Pair<Modpack, Modpack.Version> {
        val pack = reloadModpack()
        val version = pack.versions.firstOrNull { it.name == verName }
            ?: throw RequestError("版本不存在")
        return pack to version
    }

    fun Modpack.totalSize(): Long = versions.sumOf { version ->
        val versionDir = version.dir
        val sizeFile = versionDir.resolve("total_size.txt")
        when {
            sizeFile.exists() -> sizeFile.readText().toLongOrNull() ?: versionDir.safeDirSize()
            versionDir.exists() -> versionDir.safeDirSize()
            else -> 0L
        }
    }

    fun java.io.File.safeDirSize(): Long =
        if (!exists()) 0L else walkTopDown().filter { it.isFile }.sumOf { it.length() }

    fun extractOverridesRelativePath(entryName: String): String? {
        if (entryName.isBlank()) return null
        val normalized = entryName.replace('\\', '/').trim('/')
        if (normalized.isEmpty()) return null
        val segments = normalized.split('/').filter { it.isNotEmpty() }
        val overridesIndex = segments.indexOf("overrides")
        if (overridesIndex == -1) return null
        val relativeSegments = segments.drop(overridesIndex + 1)
        if (relativeSegments.isEmpty()) return null
        return relativeSegments.joinToString("/")
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