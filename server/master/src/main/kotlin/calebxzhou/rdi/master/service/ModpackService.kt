package calebxzhou.rdi.master.service

import calebxzhou.mykotutils.curseforge.CFDownloadMod
import calebxzhou.mykotutils.curseforge.CFDownloadModException
import calebxzhou.mykotutils.curseforge.CurseForgeApi
import calebxzhou.mykotutils.log.Loggers
import calebxzhou.mykotutils.std.deleteRecursivelyNoSymlink
import calebxzhou.mykotutils.std.sha1
import calebxzhou.mykotutils.std.toFixed
import calebxzhou.rdi.common.model.*
import calebxzhou.rdi.common.serdesJson
import calebxzhou.rdi.common.util.ioScope
import calebxzhou.rdi.common.util.str
import calebxzhou.rdi.master.DB
import calebxzhou.rdi.master.GAME_LIBS_DIR
import calebxzhou.rdi.master.MODPACK_DATA_DIR
import calebxzhou.rdi.master.exception.ParamError
import calebxzhou.rdi.master.exception.RequestError
import calebxzhou.rdi.master.model.McVersion
import calebxzhou.rdi.master.net.*
import calebxzhou.rdi.master.service.HostService.status
import calebxzhou.rdi.master.service.ModpackService.createVersion
import calebxzhou.rdi.master.service.ModpackService.deleteModpack
import calebxzhou.rdi.master.service.ModpackService.deleteVersion
import calebxzhou.rdi.master.service.ModpackService.getVersionFile
import calebxzhou.rdi.master.service.ModpackService.getVersionFileList
import calebxzhou.rdi.master.service.ModpackService.modpackGuardContext
import calebxzhou.rdi.master.service.ModpackService.rebuildVersion
import calebxzhou.rdi.master.service.ModpackService.requireAuthor
import calebxzhou.rdi.master.service.ModpackService.toDetailVo
import calebxzhou.rdi.master.service.PlayerService.getPlayerNames
import com.mongodb.client.model.Filters.*
import com.mongodb.client.model.Projections
import com.mongodb.client.model.UpdateOptions
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
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
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import kotlin.io.path.name

val Modpack.dir
    get() = MODPACK_DATA_DIR.resolve(_id.str)
val Modpack.libsDir
    get() = GAME_LIBS_DIR
        .resolve("${mcVer}-${modloader}")
val Modpack.Version.dir
    get() = MODPACK_DATA_DIR.resolve(modpackId.str).resolve(name)
val Modpack.Version.zip
    get() = dir.parentFile.resolve("${name}.zip")
val Modpack.Version.clientZip
    get() = dir.parentFile.resolve("${name}-client.zip")
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
                get("/client") {
                    call.modpackGuardContext().version.clientZip.let { call.respondFile(it) }
                }
                get("/client/hash") {
                    call.modpackGuardContext().version.clientZip.let { response(data = it.sha1) }
                }
                get("/mods") {
                    call.modpackGuardContext().version.mods.let { response(data = it) }
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
    private val lgr by Loggers
    private const val MAX_MODPACK_PER_USER = 5
    private val STEP_PROGRESS_REGEX = Regex("""^Step\s+(\d+)/(\d+)""")
    private val realDbcl = DB.getCollection<Modpack>("modpack")
    internal var testDbcl: MongoCollection<Modpack>? = null
    val dbcl: MongoCollection<Modpack>
        get() = testDbcl ?: realDbcl
    private val clientNeedDirs = listOf("config", "mods", "defaultconfigs", "kubejs", "global_packs", "resourcepacks")
    private val disallowedClientPaths = setOf("config/fancymenu")
    private val allowedQuestLangFiles = setOf("en_us.snbt", "zh_cn.snbt")
    private const val QUEST_LANG_PREFIX = "config/ftbquests/quests/lang/"
    private const val RESOURCEPACK_MAX_SIZE_BYTES = 1024L * 1024
    private const val PNG_COMPRESSION_THRESHOLD_BYTES = 50 * 1024
    private const val PNG_COMPRESSION_JPEG_QUALITY = 0.5f

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
        version.processMods(modpack)
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

    suspend fun Modpack.installToHost(verName: String, host: Host, onProgress: (String) -> Unit) {
        val version = getVersion(verName)
            ?: throw RequestError("整合包版本不存在: $verName")
        if (version.status != Modpack.Status.OK) {
            throw RequestError("此版本未准备好或构建失败")
        }
        val hostDir = host.dir.canonicalFile.apply { mkdirs() }
        if (!version.zip.exists()) {
            throw RequestError("版本压缩文件不存在: ${version.zip}")
        }
        onProgress("开始安装整合包..")
        unzipOverrides(version.zip, hostDir)
        onProgress("解压成功 开始下载mod")
        val hostModsDir = hostDir.resolve("mods").apply { mkdirs() }
        val mods = version.mods.filter { it.side != Mod.Side.CLIENT }
            .map { CFDownloadMod(it.projectId.toInt(), it.fileId.toInt(), it.slug,it.targetPath) }
        val downloadedMods = CurseForgeApi.downloadMods(
            mods,
        ) { cfm, prog ->
            onProgress("${cfm.slug} mod下载中 ${prog.percent.toFixed(2)}%")
        }.getOrElse {
            if (it is CFDownloadModException) {
                it.failed.forEach { (mod, ex) ->
                    onProgress("Mod $mod 下载失败:${ex.message}，安装终止")
                    ex.printStackTrace()
                }
            } else {
                it.printStackTrace()
                onProgress("未知错误:${it.message}，安装终止")

            }
            return
        }
        onProgress("mod全部下载成功 安装到主机..")
        downloadedMods.forEach { mod ->
                Files.createDirectories(mod.path.parent)
                Files.copy(
                    mod.path,
                    hostModsDir.resolve(mod.path.fileName.name).toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )

        }

        val libsSource = libsDir.canonicalFile.also {
            if (!it.exists() || !it.isDirectory) {
                throw RequestError("整合包依赖目录缺失: $it")
            }
        }
        onProgress("安装运行库..")
        copyDirectoryContents(libsSource, hostDir)
    }

    private fun copyDirectoryContents(sourceDir: File, targetDir: File) {
        val entries = sourceDir.listFiles() ?: return
        entries.forEach { entry ->
            val target = File(targetDir, entry.name)
            if (entry.isDirectory) {
                entry.copyRecursively(target, overwrite = true)
            } else {
                target.parentFile?.mkdirs()
                entry.copyTo(target, overwrite = true)
            }
        }
    }

    private fun unzipOverrides(zipFile: File, targetDir: File) {
        val versionDirPath = targetDir.toPath()
        // Use ZipFile with charset detection to handle Chinese filenames
        open(zipFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
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
                        zip.getInputStream(entry).use { input ->
                            Files.newOutputStream(
                                resolvedPath,
                                StandardOpenOption.CREATE,
                                StandardOpenOption.TRUNCATE_EXISTING
                            ).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }
        }
    }

    fun Modpack.Version.processMods(modpack: Modpack) {
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
                "7291067",
                "2392977662"
            )
        } else throw RequestError("不支持的mc版本 无法添加kotlin for forge")
        mods.removeIf { it.slug == "kotlin-for-forge" }
        mods += kffMod
        //移除备份有关的
        mods.removeIf { it.slug.contains("backup") }
        //移除fancymenu以及相关的
        mods.removeIf { it.slug.contains("fancymenu") }
        mods.removeIf { it.slug.contains("spiffyhud") }
        mods.removeIf { it.slug.contains("drippy-loading-screen") }
        mods.removeIf { it.slug.contains("tbs-main-menu-override") }
        mods.removeIf { it.slug.contains("welcome-screen") }
        //移除powerful-dummy 不兼容
        mods.removeIf { it.slug=="powerful-dummy" }
        //重度机械症c6c compatibility
        //不给这个mod服务端装上去会class not found
        mods.find { it.slug=="loot-beams-refork" }?.side = Mod.Side.BOTH
        //没有的话整理背包会卡服
        mods.find { it.slug=="inventory-profiles-next" }?.side = Mod.Side.BOTH


    }

    suspend fun Modpack.buildVersion(version: Modpack.Version, onProgress: (String) -> Unit) {
        if (!version.zip.exists()) {
            throw RequestError("版本压缩文件不存在 请重新上传")
        }
        if (version.status == Modpack.Status.BUILDING) {
            throw RequestError("版本正在构建中 请勿重复操作")
        }
        if (version.hostsUsing().isNotEmpty()) throw RequestError("有主机正在使用此版本，无法重构")
        version.setTotalSize(version.zip.length())
        //todo 删除ftb backup
        try {
            val downloadedMods = CurseForgeApi.downloadMods(version.mods.filter { it.side != Mod.Side.CLIENT }
                .map { CFDownloadMod(it.projectId.toInt(), it.fileId.toInt(),it.slug, it.targetPath) }) { cfmod, prog ->
                onProgress("mod下载中：${cfmod.slug} ${prog.percent.toFixed(2)}")
            }.getOrThrow()
            onProgress("所有mod下载完成 开始安装。。${downloadedMods.size}个mod")
            onProgress("构建客户端版。。")
            buildClientZip(version)
            onProgress("客户端版本构建完成")
            onProgress("整合包构建完成！")
            version.setStatus(Modpack.Status.OK)

        } catch (e: Exception) {
            version.setStatus(Modpack.Status.FAIL)
            throw e
        }
    }

    private fun buildClientZip(version: Modpack.Version) {
        val sourceZip = version.zip
        if (!sourceZip.exists()) return
        val clientZip = version.clientZip
        clientZip.parentFile?.mkdirs()
        if (clientZip.exists()) clientZip.delete()

        val neededDirs = clientNeedDirs.toSet()
        var entriesCopied = 0

        open(sourceZip).use { source ->
            ZipOutputStream(clientZip.outputStream()).use { output ->
                val addedDirs = mutableSetOf<String>()
                source.entries().asSequence().forEach { entry ->
                    val relative = extractOverridesRelativePath(entry.name) ?: return@forEach
                    val relativeLower = relative.lowercase()
                    if (disallowedClientPaths.any { relativeLower.startsWith(it) }) {
                        return@forEach
                    }
                    if (relativeLower.endsWith(".ogg")) {
                        return@forEach
                    }
                    if (isQuestLangEntryDisallowed(relativeLower, entry.isDirectory)) {
                        return@forEach
                    }
                    val topLevel = relative.substringBefore('/', relative)
                    if (topLevel !in neededDirs) return@forEach

                    if (entry.isDirectory) {
                        if (addDirectoryEntry(relative, output, addedDirs)) {
                            entriesCopied++
                        }
                        return@forEach
                    }

                    if (topLevel == "resourcepacks") {
                        val bytes = readResourcepackEntry(source, entry, relativeLower) ?: return@forEach
                        ensureZipParents(relative, output, addedDirs)
                        val clientEntry = ZipEntry(relative).apply { time = entry.time }
                        output.putNextEntry(clientEntry)
                        output.write(bytes)
                        output.closeEntry()
                        entriesCopied++
                    } else {
                        ensureZipParents(relative, output, addedDirs)
                        val clientEntry = ZipEntry(relative).apply { time = entry.time }
                        output.putNextEntry(clientEntry)
                        val isPng = relativeLower.endsWith(".png")
                        if (isPng) {
                            val bytes = source.getInputStream(entry).use { it.readBytes() }
                            val processed = compressPngIfNeeded(bytes)
                            output.write(processed)
                        } else {
                            source.getInputStream(entry).use { input ->
                                input.copyTo(output)
                            }
                        }
                        output.closeEntry()
                        entriesCopied++
                    }
                }
            }
        }

        if (entriesCopied == 0) {
            clientZip.delete()
        }
    }

    private fun addDirectoryEntry(
        rawPath: String,
        output: ZipOutputStream,
        addedDirs: MutableSet<String>
    ): Boolean {
        val sanitized = rawPath.trim('/').ifEmpty { return false }
        ensureZipParents(sanitized, output, addedDirs)
        val dirEntry = "$sanitized/"
        if (addedDirs.add(dirEntry)) {
            output.putNextEntry(ZipEntry(dirEntry))
            output.closeEntry()
            return true
        }
        return false
    }

    private fun ensureZipParents(path: String, output: ZipOutputStream, addedDirs: MutableSet<String>) {
        val normalized = path.trim('/').ifEmpty { return }
        val parts = normalized.split('/')
        if (parts.size <= 1) return
        var current = ""
        for (i in 0 until parts.size - 1) {
            val part = parts[i]
            if (part.isEmpty()) continue
            current = if (current.isEmpty()) part else "$current/$part"
            val dirEntry = "$current/"
            if (addedDirs.add(dirEntry)) {
                output.putNextEntry(ZipEntry(dirEntry))
                output.closeEntry()
            }
        }
    }

    private fun readBytesWithLimit(input: InputStream, limit: Long): ByteArray? {
        val buffer = ByteArrayOutputStream()
        val chunk = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = input.read(chunk)
            if (read == -1) break
            total += read
            if (total > limit) {
                return null
            }
            buffer.write(chunk, 0, read)
        }
        return buffer.toByteArray()
    }

    private fun isQuestLangEntryDisallowed(relativeLower: String, isDirectory: Boolean): Boolean {
        if (!relativeLower.startsWith(QUEST_LANG_PREFIX)) return false
        val remainder = relativeLower.removePrefix(QUEST_LANG_PREFIX)
        if (remainder.isEmpty()) return false
        if (isDirectory) return true
        if (remainder.contains('/')) return true
        return remainder !in allowedQuestLangFiles
    }

    private fun readResourcepackEntry(source: ZipFile, entry: ZipEntry, relativeLower: String): ByteArray? {
        if (entry.size != -1L && entry.size > RESOURCEPACK_MAX_SIZE_BYTES) {
            return null
        }
        val rawBytes = source.getInputStream(entry).use { input ->
            when {
                entry.size == -1L -> readBytesWithLimit(input, RESOURCEPACK_MAX_SIZE_BYTES)
                entry.size > Int.MAX_VALUE -> return null
                entry.size > RESOURCEPACK_MAX_SIZE_BYTES -> return null
                else -> input.readNBytes(entry.size.toInt())
            }
        } ?: return null
        val processed = if (relativeLower.endsWith(".png")) compressPngIfNeeded(rawBytes) else rawBytes
        if (processed.size > RESOURCEPACK_MAX_SIZE_BYTES) return null
        return processed
    }

    private fun compressPngIfNeeded(bytes: ByteArray): ByteArray {
        if (bytes.size <= PNG_COMPRESSION_THRESHOLD_BYTES) return bytes
        return runCatching {
            val original = ImageIO.read(ByteArrayInputStream(bytes)) ?: return bytes
            val rgbImage = if (original.type == BufferedImage.TYPE_INT_RGB) original else {
                val converted = BufferedImage(original.width, original.height, BufferedImage.TYPE_INT_RGB)
                val graphics = converted.createGraphics()
                graphics.color = Color.WHITE
                graphics.fillRect(0, 0, converted.width, converted.height)
                graphics.drawImage(original, 0, 0, null)
                graphics.dispose()
                converted
            }
            val writerIterator = ImageIO.getImageWritersByFormatName("jpg")
            if (!writerIterator.hasNext()) return bytes
            val writer = writerIterator.next()
            try {
                val params = writer.defaultWriteParam
                if (params.canWriteCompressed()) {
                    params.compressionMode = ImageWriteParam.MODE_EXPLICIT
                    params.compressionQuality = PNG_COMPRESSION_JPEG_QUALITY
                }
                ByteArrayOutputStream().use { baos ->
                    val imageOut = ImageIO.createImageOutputStream(baos) ?: return bytes
                    imageOut.use { outputStream ->
                        writer.output = outputStream
                        writer.write(null, IIOImage(rgbImage, null, null), params)
                    }
                    baos.toByteArray()
                }
            } finally {
                writer.dispose()
            }
        }.getOrElse { bytes }
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

    fun open(zipFile: File): ZipFile {
        var lastError: Exception? = null
        val attempted = mutableListOf<String>()

        fun tryOpen(charset: Charset?): ZipFile? {
            return try {
                if (charset == null) ZipFile(zipFile) else ZipFile(zipFile, charset)
            } catch (ex: Exception) {
                lastError = ex
                attempted += charset?.name() ?: "system-default"
                null
            }
        }

        tryOpen(null)?.let { return it }
        buildList {
            add(StandardCharsets.UTF_8)
            add(Charset.defaultCharset())
            runCatching { add(Charset.forName("GB18030")) }.getOrNull()
            runCatching { add(Charset.forName("GBK")) }.getOrNull()
            add(StandardCharsets.ISO_8859_1)
        }.filterNotNull().distinct().forEach { charset ->
            tryOpen(charset)?.let { return it }
        }

        throw RequestError(
            "无法读取整合包: ${lastError?.message ?: "未知错误"} (尝试编码: ${attempted.joinToString()})"
        )
    }
}