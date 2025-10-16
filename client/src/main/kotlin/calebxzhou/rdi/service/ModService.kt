package calebxzhou.rdi.service

import calebxzhou.rdi.Const
import calebxzhou.rdi.RDI
import calebxzhou.rdi.lgr
import calebxzhou.rdi.model.CurseForgeFingerprintData
import calebxzhou.rdi.model.CurseForgeFingerprintRequest
import calebxzhou.rdi.model.CurseForgeFingerprintResponse
import calebxzhou.rdi.model.CurseForgeMod
import calebxzhou.rdi.model.CurseForgeModsRequest
import calebxzhou.rdi.model.CurseForgeModsResponse
import calebxzhou.rdi.model.ModBriefInfo
import calebxzhou.rdi.net.httpStringRequest_
import calebxzhou.rdi.net.success
import calebxzhou.rdi.model.ModrinthProject
import calebxzhou.rdi.model.ModrinthVersionInfo
import calebxzhou.rdi.model.ModrinthVersionLookupRequest
import calebxzhou.rdi.net.httpRequest
import calebxzhou.rdi.net.json
import calebxzhou.rdi.util.json
import calebxzhou.rdi.util.murmur2
import calebxzhou.rdi.util.serdesJson
import calebxzhou.rdi.util.sha1
import com.electronwill.nightconfig.core.CommentedConfig
import com.electronwill.nightconfig.core.Config
import com.electronwill.nightconfig.toml.TomlFormat
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import java.io.File
import java.net.URI
import java.util.jar.JarFile
import kotlin.collections.contains
import kotlin.collections.firstOrNull
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer


object ModService {
    const val NEOFORGE_CONFIG_PATH = "META-INF/neoforge.mods.toml"
    const val FABRIC_CONFIG_PATH = "fabric.mod.json"
    const val FORGE_CONFIG_PATH = "META-INF/mods.toml"
    val briefInfo: List<ModBriefInfo> by lazy { loadBriefInfo() }
    val cfSlugBriefInfo: Map<String, ModBriefInfo> by lazy { buildSlugMap(briefInfo) { it.curseforgeSlugs } }
    val mrSlugBriefInfo: Map<String, ModBriefInfo> by lazy { buildSlugMap(briefInfo) { it.modrinthSlugs } }

    private data class FileFingerprint(val path: String, val length: Long, val lastModified: Long)

    private data class HashCacheEntry(
        val lastModified: Long,
        val length: Long,
        val sha1: String? = null,
        val murmur2: Int? = null
    )

    private val hashCacheLock = Any()
    private val hashCache = mutableMapOf<String, HashCacheEntry>()

    private val sha1Lock = Any()
    @Volatile private var cachedSha1Snapshot: List<FileFingerprint>? = null
    @Volatile private var cachedSha1Map: Map<String, File> = emptyMap()

    private val murmur2Lock = Any()
    @Volatile private var cachedMurmur2Snapshot: List<FileFingerprint>? = null
    @Volatile private var cachedMurmur2Map: Map<Int, File> = emptyMap()

    private const val MCMOD_CACHE_TTL_MS = 7L * 24 * 60 * 60 * 1000
    private val mcmodCacheRoot by lazy { File(File(RDI.DIR, "cache"), "mcmod").apply { mkdirs() } }

    private fun mcmodCacheFile(url: String): File? {
        val path = runCatching { URI(url).path }.getOrNull()?.trimStart('/') ?: return null
        val safeSegments = path.split('/')
            .filter { it.isNotBlank() && it != "." && it != ".." }
        if (safeSegments.isEmpty()) {
            return null
        }
        val relativePath = safeSegments.joinToString(File.separator)
        return File(mcmodCacheRoot, relativePath)
    }

    private fun loadBriefInfo(): List<ModBriefInfo> {
        val resourcePath = "mod_brief_info.json"
        val raw = runCatching {
            ModService::class.java.classLoader.getResourceAsStream(resourcePath)?.bufferedReader()?.use { it.readText() }
        }.onFailure {
            lgr.error("Failed to read $resourcePath", it)
        }.getOrNull()

        if (raw.isNullOrBlank()) {
            lgr.warn("mod_brief_info.json is missing or empty; fallback to empty brief info list")
            return emptyList()
        }

        return runCatching { serdesJson.decodeFromString<List<ModBriefInfo>>(raw) }
            .onFailure { err -> lgr.error("Failed to decode mod_brief_info.json", err) }
            .getOrElse { emptyList() }
    }

    private fun buildSlugMap(
        data: List<ModBriefInfo>,
        slugSelector: (ModBriefInfo) -> List<String>
    ): Map<String, ModBriefInfo> {
        val map = linkedMapOf<String, ModBriefInfo>()
        data.forEach { info ->
            slugSelector(info)
                .asSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { slug ->
                    val normalized = slug.lowercase()
                    val previous = map.put(normalized, info)
                    if (previous != null && previous !== info) {
                        lgr.debug("Duplicated slug '$slug' now mapped to ${info.mcmodId}, previously ${previous.mcmodId}")
                    }
                }
        }
        return map
    }

    private fun List<File>.toSnapshot(): List<FileFingerprint> = map {
        FileFingerprint(it.absolutePath, it.length(), it.lastModified())
    }.sortedBy { it.path }

    private fun cleanupHashCache(currentFiles: List<File>) {
        val activeKeys = currentFiles.mapTo(hashSetOf()) { it.absolutePath }
        synchronized(hashCacheLock) {
            val iterator = hashCache.entries.iterator()
            while (iterator.hasNext()) {
                if (iterator.next().key !in activeKeys) {
                    iterator.remove()
                }
            }
        }
    }

    private fun File.cachedSha1(): String {
        val key = absolutePath
        val currentModified = lastModified()
        val currentLength = length()

        synchronized(hashCacheLock) {
            val cached = hashCache[key]
            if (cached != null && cached.lastModified == currentModified && cached.length == currentLength && cached.sha1 != null) {
                return cached.sha1
            }
        }

        val computed = sha1

        synchronized(hashCacheLock) {
            val existing = hashCache[key]
            val entry = if (existing != null && existing.lastModified == currentModified && existing.length == currentLength) {
                existing.copy(sha1 = computed)
            } else {
                HashCacheEntry(currentModified, currentLength, sha1 = computed, murmur2 = existing?.murmur2)
            }
            hashCache[key] = entry
        }

        return computed
    }

    private fun File.cachedMurmur2(): Int {
        val key = absolutePath
        val currentModified = lastModified()
        val currentLength = length()

        synchronized(hashCacheLock) {
            val cached = hashCache[key]
            if (cached != null && cached.lastModified == currentModified && cached.length == currentLength && cached.murmur2 != null) {
                return cached.murmur2
            }
        }

        val computed = murmur2

        synchronized(hashCacheLock) {
            val existing = hashCache[key]
            val entry = if (existing != null && existing.lastModified == currentModified && existing.length == currentLength) {
                existing.copy(murmur2 = computed)
            } else {
                HashCacheEntry(currentModified, currentLength, sha1 = existing?.sha1, murmur2 = computed)
            }
            hashCache[key] = entry
        }

        return computed
    }

    private fun JarFile.readNeoForgeConfig(): CommentedConfig? {
        return getJarEntry(NEOFORGE_CONFIG_PATH)?.let { modsTomlEntry ->
            getInputStream(modsTomlEntry).bufferedReader().use { reader ->
                val parsed = TomlFormat.instance()
                    .createParser()
                    .parse(reader.readText())
                parsed
            }
        }
    }
    val JarFile.logo
        get() =
         getJarEntry("logo.png")?.let { logoEntry ->
             getInputStream(logoEntry).readBytes()
         }


    private val CommentedConfig.modId
        get() = get<List<Config>>("mods").firstOrNull()?.get<String>("modId")!!

    val MOD_DIR
        get() = System.getProperty("rdi.modDir")?.let { File(it) } ?: File("mods")
    val mcmodSearchUrl = "https://search.mcmod.cn/s?key="
    val mcmodHeader
        get() =
            """  
Host: search.mcmod.cn
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:137.0) Gecko/20100101 Firefox/137.0
Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
Accept-Language: zh-CN,zh;q=0.5
Accept-Encoding: 1
Connection: keep-alive
Cookie: MCMOD_SEED=${
                (1..26)
                    .map { ('a'..'z') + ('0'..'9').random() } // Pick a random character from the allowed set
                    .joinToString("")
            }; search_history_list=
Upgrade-Insecure-Requests: 1
Sec-Fetch-Dest: document
Sec-Fetch-Mode: navigate
Sec-Fetch-Site: none
Sec-Fetch-User: ?1
Priority: u=0, i
        """.trimIndent().split("\n").map { it.split(": ") }.map { it[0] to it[1] }

    fun forEachMod(action: (File, JarFile) -> Unit) {
        mods.forEach { jarFile ->
            JarFile(jarFile).use { jar ->
                action(jarFile, jar)
            }
        }
    }

    val mods
        get() = MOD_DIR.listFiles { it.extension == "jar" }?.toList() ?: listOf()
    val sha1Mods: Map<String, File>
        get() {
            val files = mods
            if (files.isEmpty()) {
                synchronized(sha1Lock) {
                    cachedSha1Snapshot = emptyList()
                    cachedSha1Map = emptyMap()
                }
                cleanupHashCache(files)
                return emptyMap()
            }

            val snapshot = files.toSnapshot()
            synchronized(sha1Lock) {
                if (snapshot == cachedSha1Snapshot) {
                    return cachedSha1Map
                }
            }

            val computed = files.associateBy { it.cachedSha1() }
            cleanupHashCache(files)

            synchronized(sha1Lock) {
                cachedSha1Snapshot = snapshot
                cachedSha1Map = computed
            }

            return computed
        }

    val murmur2Mods: Map<Int, File>
        get() {
            val files = mods
            if (files.isEmpty()) {
                synchronized(murmur2Lock) {
                    cachedMurmur2Snapshot = emptyList()
                    cachedMurmur2Map = emptyMap()
                }
                cleanupHashCache(files)
                return emptyMap()
            }

            val snapshot = files.toSnapshot()
            synchronized(murmur2Lock) {
                if (snapshot == cachedMurmur2Snapshot) {
                    return cachedMurmur2Map
                }
            }

            val computed = files.associateBy { it.cachedMurmur2() }
            cleanupHashCache(files)

            synchronized(murmur2Lock) {
                cachedMurmur2Snapshot = snapshot
                cachedMurmur2Map = computed
            }

            return computed
        }
    val idMods
        get() = mods.mapNotNull { file ->
                JarFile(file).use { jar ->
                    jar.readNeoForgeConfig()?.let { conf ->
                        conf.modId to file
                    }
                }

        }.toMap()
    val idSha1s
        get() = mods.mapNotNull { file ->
                JarFile(file).use { jar ->
                    jar.readNeoForgeConfig()?.let { conf ->
                        conf.modId to file.sha1
                    }
                }
        }.toMap()
    val idNames
        get() = mods.mapNotNull { file ->
                JarFile(file).use { jar ->
                    jar.readNeoForgeConfig()?.let { conf ->
                        conf.modId to conf.get<List<Config>>("mods").firstOrNull()?.get<String>("displayName")!!
                    }
                }
        }.toMap()

    suspend fun getFingerprintsCurseForge(): CurseForgeFingerprintData {
    val fingerprints = mods.map { it.cachedMurmur2() }

        val response = httpRequest {
            url("https://api.curseforge.com/v1/fingerprints/432")
            method= HttpMethod.Post
            json()
            setBody(CurseForgeFingerprintRequest(fingerprints = fingerprints))
            header("x-api-key", Const.CF_AKEY)
        }.body<CurseForgeFingerprintResponse>()

        val data = response.data?: CurseForgeFingerprintData()
        lgr.info(
            "CurseForge: ${data.exactMatches.size} exact matches, ${data.partialMatches.size} partial matches, ${data.unmatchedFingerprints.size} unmatched"
        )

        return data
    }
    suspend fun getInfosCurseForge(cfModIds: List<Long>): List<CurseForgeMod> {

        if (cfModIds.isEmpty()) {
            lgr.info("CurseForge mod lookup skipped: empty mod ID list")
            return emptyList()
        }

        val requestPayload = CurseForgeModsRequest(
            modIds = cfModIds,
            filterPcOnly = true
        )

        val response = httpStringRequest_(
            post = true,
            url = "https://api.curseforge.com/v1/mods",
            jsonBody = requestPayload.json
        )

        if (!response.success) {
            lgr.warn("CurseForge mod lookup failed: HTTP ${response.statusCode()} ${response.body()}")
            return emptyList()
        }

        val body = response.body()
        if (body.isNullOrBlank()) {
            lgr.warn("CurseForge mod lookup returned empty body")
            return emptyList()
        }

        val decoded = runCatching {
            serdesJson.decodeFromString<CurseForgeModsResponse>(body)
        }.onFailure { err ->
            lgr.error("Failed to parse CurseForge mods response", err)
        }.getOrNull()

        val mods = decoded?.data
        if (mods == null) {
            lgr.warn("CurseForge mods response missing data field")
            return emptyList()
        }

        lgr.info("CurseForge: fetched ${mods.size} mods for ${cfModIds.size} requested IDs")
        return mods
    }
    suspend fun getVersionsModrinth(): Map<String, ModrinthVersionInfo> {

        if (mods.isEmpty()) {
            lgr.info("Modrinth lookup skipped: no mods found in ${MOD_DIR.absolutePath}")
            return emptyMap()
        }

    val hashes = mods.map { it.cachedSha1() }
        val requestPayload = ModrinthVersionLookupRequest(hashes = hashes, algorithm = "sha1")

        val response = httpRequest{
            url("https://api.modrinth.com/v2/version_files")
            method= HttpMethod.Post
            json()
            setBody(requestPayload)
        }.body<Map<String, ModrinthVersionInfo>>()

        val missing = hashes.filter { it !in response }
        if (missing.isNotEmpty()) {
            lgr.info("Modrinth: ${response.size} matches, ${missing.size} hashes unmatched")
        } else {
            lgr.info("Modrinth: matched all ${response.size} hashes")
        }

        return response
    }
    suspend fun getProjectsModrinth(ids: List<String>): List<ModrinthProject> {

        val normalizedIds = ids.asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .toList()

        if (normalizedIds.isEmpty()) {
            lgr.info("Modrinth project lookup skipped: empty id list")
            return emptyList()
        }

        val chunkSize = 100
        val projects = mutableListOf<ModrinthProject>()

        normalizedIds.chunked(chunkSize).forEach { chunk ->
            val encodedIds = serdesJson.encodeToString(ListSerializer(String.serializer()), chunk)

            val response = httpRequest {
                url("https://api.modrinth.com/v2/projects")
                method = HttpMethod.Get
                parameter("ids", encodedIds)
            }

            val fetched = runCatching { response.body<List<ModrinthProject>>() }
                .onFailure { err ->
                    lgr.error("Modrinth project lookup failed for chunk of ${chunk.size} ids", err)
                }
                .getOrElse { emptyList() }

            projects += fetched

            if (fetched.size != chunk.size) {
                val missing = chunk.toSet() - fetched.map { it.id }.toSet() - fetched.map { it.slug }.toSet()
                if (missing.isNotEmpty()) {
                    lgr.debug("Modrinth: ${missing.size} ids from chunk unmatched: ${missing.joinToString()}")
                }
            }
        }

        lgr.info("Modrinth: fetched ${projects.size} projects for ${normalizedIds.size} requested ids")

        return projects
    }

    /*suspend fun getInfoMcmod(modId: String,modName: String): McmodInfo? {

        val modUrl = httpStringRequest(url = "${mcmodSearchUrl}${"$modId $modName".urlEncoded}", headers = mcmodHeader).body
            .let { Jsoup.parse(it) }
            .select(".result-item>.head>a").firstOrNull()
            ?.attr("href")
            ?: let {
                lgr.warn("mcmod未找到mod信息，关键词：$modId $modName")
                return null
            }
        val cacheFile = mcmodCacheFile(modUrl)
        val cachedBody = cacheFile?.takeIf { it.exists() }?.let { file ->
            runCatching { file.readText(StandardCharsets.UTF_8) }
                .onFailure { err -> lgr.warn("读取mcmod缓存失败: ${file.absolutePath}", err) }
                .getOrNull()
        }
        val cacheExpired = cacheFile?.let { !it.exists() || System.currentTimeMillis() - it.lastModified() > MCMOD_CACHE_TTL_MS } ?: true

        val modBody = if (!cacheExpired && !cachedBody.isNullOrBlank()) {
            cachedBody
        } else {
            val modHost = runCatching { URI(modUrl).host }.getOrNull()
            val detailHeaders = if (modHost.isNullOrBlank()) {
                mcmodHeader
            } else {
                mcmodHeader.map { (key, value) ->
                    if (key.equals("Host", ignoreCase = true)) key to modHost else key to value
                }
            }

            val modResponse = httpStringRequest(url = modUrl, headers = detailHeaders)

            if (!modResponse.success) {
                if (!cachedBody.isNullOrBlank()) {
                    lgr.warn("mcmod详情页请求失败: HTTP ${modResponse.statusCode()} url=$modUrl，使用本地缓存")
                    cachedBody
                } else {
                    lgr.warn("mcmod详情页请求失败: HTTP ${modResponse.statusCode()} url=$modUrl")
                    return null
                }
            } else {
                val networkBody = modResponse.body()
                if (networkBody.isNullOrBlank()) {
                    if (!cachedBody.isNullOrBlank()) {
                        lgr.warn("mcmod详情页返回空内容，url=$modUrl，使用本地缓存")
                        cachedBody
                    } else {
                        lgr.warn("mcmod详情页返回空内容，url=$modUrl")
                        return null
                    }
                } else {
                    cacheFile?.let { file ->
                        runCatching {
                            file.parentFile?.mkdirs()
                            file.writeText(networkBody, StandardCharsets.UTF_8)
                        }.onFailure { err ->
                            lgr.warn("mcmod详情页缓存写入失败: ${file.absolutePath}", err)
                        }
                    }
                    networkBody
                }
            }
        }

        if (modBody.isNullOrBlank()) {
            lgr.warn("mcmod详情页内容为空，url=$modUrl")
            return null
        }

        val document = Jsoup.parse(modBody, modUrl)

        val classTitle = document.selectFirst(".class-text .class-title")
        val nameCn = classTitle?.selectFirst("h3")?.text()?.trim().orEmpty()
        val englishName = classTitle?.selectFirst("h4")?.text()?.trim().orEmpty()
        val resolvedName = englishName.ifBlank { nameCn }

        val categories = document
            .select(".class-text-top .common-class-category li a")
            .map { it.text().trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        val introElement = document.selectFirst("div.class-menu-main[data-frame=\"2\"] li.text-area")
        val intro = introElement?.let { element ->
            element.select("script,style").remove()
            val lines = element
                .select("p,li")
                .map { it.text().trim() }
                .filter { it.isNotEmpty() }
            when {
                lines.isNotEmpty() -> lines.joinToString("\n")
                else -> element.text().trim()
            }
        } ?: ""

        val authors = document
            .select("li.col-lg-12.author div.frame ul li")
            .mapNotNull { authorLi ->
                val displayName =
                    authorLi.selectFirst(".member .name")?.text()?.trim().orEmpty()
                if (displayName.isBlank()) {
                    return@mapNotNull null
                }
                val avatarUrl =
                    authorLi.selectFirst(".avatar img")?.absUrl("src")?.trim().orEmpty()
                val role = authorLi.selectFirst(".member .position")?.text()?.trim().orEmpty()

                ModAuthor(
                    name = displayName,
                    avatarUrl = avatarUrl,
                    role = role
                )
            }
        val logoUrl = document.select(".class-cover-image img").firstOrNull()?.absUrl("src")?.trim().orEmpty()
        return McmodInfo(
            pageUrl = modUrl,
            logoUrl = logoUrl,
            name = resolvedName.ifBlank { modName },
            nameCn = nameCn.ifBlank { resolvedName.ifBlank { modName } },
            categories = categories,
            intro = intro,
            authors = authors
        )

    }*/
    fun readPCLModData(){

    }

}

