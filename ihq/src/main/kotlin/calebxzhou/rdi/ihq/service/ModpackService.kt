package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.DB
import calebxzhou.rdi.ihq.exception.ParamError
import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.lgr
import calebxzhou.rdi.ihq.model.Host
import calebxzhou.rdi.ihq.model.HostStatus
import calebxzhou.rdi.ihq.model.McVersion
import calebxzhou.rdi.ihq.model.RAccount
import calebxzhou.rdi.ihq.model.pack.Mod
import calebxzhou.rdi.ihq.model.pack.Modpack
import calebxzhou.rdi.ihq.model.pack.ModpackDetailedVo
import calebxzhou.rdi.ihq.model.pack.ModpackVo
import calebxzhou.rdi.ihq.net.*
import calebxzhou.rdi.ihq.service.HostService.SERVER_RDI_CORE_FILENAME
import calebxzhou.rdi.ihq.service.HostService.status
import calebxzhou.rdi.ihq.service.ModpackService.createVersion
import calebxzhou.rdi.ihq.service.ModpackService.deleteModpack
import calebxzhou.rdi.ihq.service.ModpackService.deleteVersion
import calebxzhou.rdi.ihq.service.ModpackService.getVersionFile
import calebxzhou.rdi.ihq.service.ModpackService.getVersionFileList
import calebxzhou.rdi.ihq.service.ModpackService.modpackGuardContext
import calebxzhou.rdi.ihq.service.ModpackService.rebuildVersion
import calebxzhou.rdi.ihq.service.ModpackService.requireAuthor
import calebxzhou.rdi.ihq.service.ModpackService.toDetailVo
import calebxzhou.rdi.ihq.service.PlayerService.getPlayerNames
import calebxzhou.rdi.ihq.util.deleteRecursivelyNoSymlink
import calebxzhou.rdi.ihq.util.humanSize
import calebxzhou.rdi.ihq.util.ioScope
import calebxzhou.rdi.ihq.util.safeDirSize
import calebxzhou.rdi.ihq.util.serdesJson
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Projections
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import com.sun.org.apache.bcel.internal.Const
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.io.readByteArray
import org.bson.Document
import org.bson.types.ObjectId
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.zip.ZipInputStream
import kotlin.io.path.absolute


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

            get {
                response(data = call.modpackGuardContext().modpack.toDetailVo())
            }

            delete {
                call.modpackGuardContext().requireAuthor().deleteModpack()
                ok()
            }

            route("/version/{verName}") {
                get {
                    call.modpackGuardContext().versionNull?.let { response(data = it) }
                        ?: throw RequestError("无此版本")
                }
                delete {
                    call.modpackGuardContext().requireAuthor().deleteVersion()
                    ok()

                }
                post("/rebuild") {
                    call.modpackGuardContext().requireAuthor().rebuildVersion()

                    ok()

                }
                post {
                    val ctx = call.modpackGuardContext()
                    val verName = param("verName").trim()
                    //limit 1 GB
                    val multipart = call.receiveMultipart(formFieldLimit = 1024 * 1024 * 1024)
                    var zipBytes: ByteArray? = null
                    var mods: MutableList<Mod>? = null

                    while (true) {
                        val part = multipart.readPart() ?: break
                        when (part) {
                            is PartData.FormItem -> if (part.name == "mods") {
                                mods = runCatching { serdesJson.decodeFromString<MutableList<Mod>>(part.value) }
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
                    val modList = mods ?: arrayListOf()

                    ctx.requireAuthor().createVersion(verName, payload, modList)
                    ok()

                }
                get("/files") {
                    call.modpackGuardContext().getVersionFileList().let { response(data = it) }
                }
                get("/file/{filePath...}") {

                    val filePath = call.parameters.getAll("filePath")?.joinToString("/")
                        ?: throw ParamError("缺少参数: filePath")
                    val content = call.modpackGuardContext().getVersionFile(filePath)
                    call.respondBytes(content)

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

class ModpackContext(
    val player: RAccount,
    val modpack: Modpack,
    val versionNull: Modpack.Version?
) {
    val version get() = versionNull ?: throw ParamError("缺少版本信息")
}

object ModpackService {
    private const val MAX_MODPACK_PER_USER = 5
    private val STEP_PROGRESS_REGEX = Regex("""^Step\s+(\d+)/(\d+)""")
    val dbcl = DB.getCollection<Modpack>("modpack")


    val ModpackContext.isAuthor: Boolean
        get() = modpack.authorId == player._id

    fun ModpackContext.requireAuthor(): ModpackContext {
        if (!isAuthor) throw RequestError("不是你的整合包")
        return this
    }

    fun Modpack.isMcVer(ver: McVersion): Boolean {
        return mcVer == ver.verStr
    }

    suspend fun ApplicationCall.modpackGuardContext(): ModpackContext {
        val requesterId = uid
        val player = PlayerService.getById(requesterId) ?: throw RequestError("用户不存在")
        val modpack = ModpackService.dbcl.find(eq("_id", idParam("modpackId"))).firstOrNull()
            ?: throw RequestError("整合包不存在")
        val verName = paramNull("verName")?.trim()?.takeIf { it.isNotEmpty() }
        val version = verName?.let { name ->
            modpack.versions.firstOrNull { it.name == name }
        }
        return ModpackContext(player, modpack, version)
    }

    suspend fun listByAuthor(uid: ObjectId): List<Modpack> = dbcl.find(eq("authorId", uid)).toList()

    suspend fun getById(id: ObjectId): Modpack? = dbcl.find(eq("_id", id)).firstOrNull()

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
                fileSize = pack.versions.lastOrNull()?.totalSize ?: 0L,
                icon = pack.icon,
                info = pack.info
            )
        }
    }

    //单个整合包的详细信息
    suspend fun Modpack.toDetailVo(): ModpackDetailedVo {
        val authorName = PlayerService.getName(authorId) ?: "未知作者"
        return ModpackDetailedVo(
            id = _id,
            name = name,
            authorId = authorId,
            authorName = authorName,
            modCount = versions.maxOfOrNull { it.mods.size } ?: 0,
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
            throw RequestError("一个人最多传5个包")
        }
        if (dbcl.countDocuments(eq(Modpack::name.name, name)) > 0) {
            throw RequestError("别人传过这个包了")
        }
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

    suspend fun ModpackContext.rebuildVersion() {
        ioScope.launch {
            runCatching {
                version.setStatus(Modpack.Status.BUILDING)
                val mailId = MailService.sendSystemMail(
                    player._id,
                    "重构整合包：${modpack.name} V${version.name}",
                    "开始重新构建整合包 ${modpack.name} 版本${version.name}\n"
                )._id
                modpack.buildVersion(version) {
                    lgr.info { it }
                    MailService.changeMail(mailId, newContent = it)
                }
            }.onFailure { error ->
                lgr.error(error) { "重构整合包 ${modpack._id}:${version.name} 失败" }
                version.setStatus(Modpack.Status.FAIL)
            }
        }
    }

    suspend fun Modpack.getVersion(verName: String): Modpack.Version? {
        return versions.find { it.name == verName }
    }

    suspend fun getVersion(modpackId: ObjectId, verName: String): Modpack.Version? {
        return dbcl.find(
            and(
                eq("_id", modpackId),
                elemMatch(Modpack::versions.name, eq(Modpack.Version::name.name, verName))
            )
        )
            .projection(
                Projections
                    .elemMatch(
                        Modpack::versions.name,
                        eq(Modpack.Version::name.name, verName)
                    )
            )
            .first().versions.firstOrNull()
    }

    fun ModpackContext.getVersionFile(filePath: String): ByteArray {
        if (filePath.isBlank()) throw RequestError("文件路径不能为空")
        val versionDir = version.dir
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

    fun ModpackContext.getVersionFileList(): List<String> {
        val dir = version.dir
        if (!dir.exists() || !dir.isDirectory) {
            throw RequestError("版本目录不存在")
        }
        return dir.walkTopDown()
            .filter { it.isFile }
            .map { it.relativeTo(dir).invariantSeparatorsPath }
            .toList()
    }

    suspend fun ModpackContext.createVersion(verName: String, zipBytes: ByteArray, mods: MutableList<Mod>) {
        if (verName.isBlank()) throw RequestError("版本名不能为空")
        if (modpack.versions.any { it.name == verName }) {
            throw RequestError("版本已存在")
        }

        val version = Modpack.Version(
            modpackId = modpack._id,
            name = verName,
            changelog = "新上传",
            status = Modpack.Status.WAIT,
            mods = mods,
            time = System.currentTimeMillis()
        )
        version.dir.mkdirs()
        version.zip.writeBytes(zipBytes)

        ioScope.launch {
            dbcl.updateOne(
                eq("_id", modpack._id),
                Updates.push(Modpack::versions.name, version)
            )
            lgr.info { "开始构建 ${modpack.name}:${version.name}" }
            val mailId = MailService.sendSystemMail(
                player._id,
                "整合包${verName}构建中",
                "开始构建整合包 ${modpack.name} 版本${version.name}\n"
            )._id
            runCatching {
                modpack.buildVersion(version) {
                    lgr.info { it }
                    MailService.changeMail(mailId, newContent = it)
                }
            }
                .onFailure { error ->
                    lgr.error(error) { "构建失败 ${modpack.name}:${version.name}" }
                    MailService.changeMail(
                        mailId,
                        "整合包构建失败：${modpack.name}",
                        "无法构建整合包，错误原因：${error.message}"
                    )
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

    fun Modpack.Version.addKotlinForForge(modpack: Modpack) {
        //升级kff到最新版
        val kffMod = if (modpack.isMcVer(McVersion.V211)) {
            Mod(
                "cf", "351264", "kotlin-for-forge",
                "6994056",
                "3853038505"
            )
        } else if (modpack.isMcVer(McVersion.V201)) {
            Mod(
                "cf", "351264", "kotlin-for-forge",
                "5402061",
                "598972634"
            )
        } else throw RequestError("不支持的mc版本 无法添加kotlin for forge")
        mods.removeIf { it.slug == "kotlin-for-forge" }
        mods += kffMod
    }

    suspend fun Modpack.buildVersion(version: Modpack.Version, onProgress: (String) -> Unit) {
        if (!version.zip.exists()) {
            throw RequestError("版本压缩文件不存在 请重新上传")
        }
        if (version.status == Modpack.Status.BUILDING) {
            throw RequestError("版本正在构建中 请勿重复操作")
        }
        if(version.hostsUsing().isNotEmpty()) throw RequestError("有主机正在使用此版本，无法重构")
        val targetDir = version.dir
        if (targetDir.exists()) {
            // Safely delete directory contents without following symbolic links
            targetDir.deleteRecursivelyNoSymlink()
            targetDir.mkdirs()
        } else {
            if (!targetDir.mkdirs()) {
                throw RequestError("创建版本目录失败")
            }
        }
        version.addKotlinForForge(this)
        try {
            onProgress("正在解压...")
            unzipOverrides(version.zip, targetDir)
            val modsDir = targetDir.resolve("mods").apply { mkdirs() }
            targetDir.safeDirSize.let {
                version.setTotalSize(it)
                onProgress("解压完成 总尺寸${it.humanSize}")
            }
            val downloadedMods = CurseForgeService.downloadMods(version.mods.filter { it.side != Mod.Side.CLIENT }) {
                ioScope.launch {
                    onProgress(it)
                }
            }
            onProgress("所有mod下载完成 开始安装。。${downloadedMods.size}个mod")
            //不需要复制libraries了 运行时mount到host里
            downloadedMods.forEach { sourcePath ->
                val fileName = sourcePath.fileName?.toString() ?: return@forEach
                val absoluteSource = sourcePath.toAbsolutePath()
                val targetPath = modsDir.resolve(fileName).toPath()
                targetPath.parent?.let { Files.createDirectories(it) }
                if (Files.exists(targetPath)) {
                    Files.delete(targetPath)
                }
                Files.copy(absoluteSource,targetPath)
            }
            onProgress("整合包构建完成！")
            version.setStatus(Modpack.Status.OK)

        } catch (e: Exception) {
            version.setStatus(Modpack.Status.FAIL)
            throw e
        }
    }

    suspend fun Modpack.Version.setStatus(status: Modpack.Status) {
        dbcl.updateOne(
            eq(Modpack::_id.name, modpackId),
            Updates.set("${Modpack::versions.name}.$[elem].${Modpack.Version::status.name}", status),
            UpdateOptions().arrayFilters(
                listOf(
                    Document("elem.name", name)
                )
            )
        )
    }

    suspend fun Modpack.Version.setTotalSize(size: Long) {
        dbcl.updateOne(
            eq(Modpack::_id.name, modpackId),
            Updates.set("${Modpack::versions.name}.$[elem].${Modpack.Version::totalSize.name}", size),
            UpdateOptions().arrayFilters(
                listOf(
                    Document("elem.name", name)
                )
            )
        )
    }

    suspend fun Modpack.Version.hostsUsing(): List<Host> {
        return HostService.findByModpackVersion(modpackId, name).filter { it.status != HostStatus.STOPPED }
    }
    suspend fun Modpack.hostsUsing(): List<Host> {
        return HostService.findByModpack(_id).filter { it.status != HostStatus.STOPPED }
    }

    suspend fun ModpackContext.deleteVersion() {
        if (versionNull == null) throw RequestError("无此版本")
        val hostsUsing = version.hostsUsing()
        if (hostsUsing.isNotEmpty()) throw RequestError("以下主机用了此版本整合包，且正在运行，无法删除：${hostsUsing.map { it.name }}")
        versionNull.dir.deleteRecursivelyNoSymlink()
        versionNull.zip.delete()
        dbcl.updateOne(
            eq("_id", modpack._id),
            Updates.pull(Modpack::versions.name, eq(Modpack.Version::name.name, versionNull.name))
        )
    }

    suspend fun ModpackContext.deleteModpack() {
        val hostsUsing = modpack.hostsUsing()
        if (hostsUsing.isNotEmpty()) throw RequestError("以下主机用了此整合包，且正在运行，无法删除：${hostsUsing.map { it.name }}")
        modpack.dir.deleteRecursivelyNoSymlink()
        dbcl.deleteOne(eq("_id", modpack._id))
    }


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


}