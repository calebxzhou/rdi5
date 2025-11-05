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
import calebxzhou.rdi.model.ModBriefVo
import calebxzhou.rdi.model.pack.Mod
import calebxzhou.rdi.model.ModrinthProject
import calebxzhou.rdi.model.ModrinthVersionInfo
import calebxzhou.rdi.model.ModrinthVersionLookupRequest
import calebxzhou.rdi.net.httpRequest
import calebxzhou.rdi.net.json
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


class ModService {
    companion object{

        const val NEOFORGE_CONFIG_PATH = "META-INF/neoforge.mods.toml"
        const val FABRIC_CONFIG_PATH = "fabric.mod.json"
        const val FORGE_CONFIG_PATH = "META-INF/mods.toml"
        val MOD_DIR  = System.getProperty("rdi.modDir")?.let { File(it) } ?: File("mods")
        val JarFile.modLogo
            get() =
                getJarEntry("logo.png")?.let { logoEntry ->
                    getInputStream(logoEntry).readBytes()
                }

        fun ModBriefInfo.toVo(modFile: File? = null): ModBriefVo {
            val iconBytes = modFile?.let {
                runCatching { JarFile(it).use { jar -> jar.modLogo } }.getOrNull()
            }
            return ModBriefVo(
                name = name,
                nameCn = nameCn,
                intro = intro,
                iconData = iconBytes,
                iconUrls = buildList {
                    if (logoUrl.isNotBlank()) add(logoUrl)
                }
            )
        }
    }
    val briefInfo: List<ModBriefInfo> by lazy { loadBriefInfo() }
    val cfSlugBriefInfo: Map<String, ModBriefInfo> by lazy { buildSlugMap(briefInfo) { it.curseforgeSlugs } }
    val mrSlugBriefInfo: Map<String, ModBriefInfo> by lazy { buildSlugMap(briefInfo) { it.modrinthSlugs } }

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
            ModService::class.java.classLoader.getResourceAsStream(resourcePath)?.bufferedReader()
                ?.use { it.readText() }
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


    fun JarFile.readNeoForgeConfig(): CommentedConfig? {
        return getJarEntry(NEOFORGE_CONFIG_PATH)?.let { modsTomlEntry ->
            getInputStream(modsTomlEntry).bufferedReader().use { reader ->
                val parsed = TomlFormat.instance()
                    .createParser()
                    .parse(reader.readText())
                parsed
            }
        }
    }




     val CommentedConfig.modId
        get() = get<List<Config>>("mods").firstOrNull()?.get<String>("modId")!!
     val CommentedConfig.modDescription
        get() = get<List<Config>>("mods").firstOrNull()?.get<String>("description")!!


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

    var mods = MOD_DIR.listFiles { it.extension == "jar" }?.toMutableList() ?: mutableListOf()


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
    fun filterServerOnlyMods(): ModService {
        mods = mods.filterNot { file ->
            JarFile(file).use { jar ->
                val config = jar.readNeoForgeConfig() ?: return@use false
                val modEntries = config.get("mods") as? List<Config> ?: return@use false

                modEntries.any { modConfig ->
                    val modId = modConfig.get<String>("modId")?.trim()?.lowercase() ?: return@any false
                    val dependencyKey = "dependencies.$modId"
                    val dependencies = config.get(dependencyKey) as? List<Config> ?: return@any false

                    dependencies.any { dependency ->
                        val dependencyModId = dependency.get<String>("modId")?.lowercase()
                        val side = dependency.get<String>("side")?.uppercase()
                        dependencyModId == "minecraft" && side == "CLIENT"
                    }
                }
            }
        }.toMutableList()

        return this
    }
    suspend fun getFingerprintsCurseForge(): CurseForgeFingerprintData {
        val fingerprints = mods.map { it.murmur2 }
        val response = httpRequest {
            url("https://api.curseforge.com/v1/fingerprints/432")
            method = HttpMethod.Post
            json()
            setBody(CurseForgeFingerprintRequest(fingerprints = fingerprints))
            header("x-api-key", Const.CF_AKEY)
        }.body<CurseForgeFingerprintResponse>()
        val data = response.data ?: CurseForgeFingerprintData()
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


        val mods = httpRequest {
            method = HttpMethod.Post
            json()
            url("https://api.curseforge.com/v1/mods")
            header("x-api-key", Const.CF_AKEY)
            setBody(
                CurseForgeModsRequest(
                    modIds = cfModIds,
                    filterPcOnly = true
                )
            )
        }.body<CurseForgeModsResponse>().data
        if (mods == null) {
            lgr.warn("CurseForge mods response missing data field")
            return emptyList()
        }

        lgr.info("CurseForge: fetched ${mods.size} mods for ${cfModIds.size} requested IDs")
        return mods
    }

    private fun logUnmatched(source: String, unmatched: Collection<File>) {
        if (unmatched.isEmpty()) {
            lgr.info("$source: 所有本地 Mod 均已匹配")
        } else {
            lgr.info(
                "$source: 未匹配到 ${unmatched.size} 个 Mod: " +
                        unmatched.joinToString(", ") { it.name }
            )
        }
    }


    private fun CurseForgeMod.toBriefVo(modFile: File?): ModBriefVo {
        val icons = buildList {
            logo?.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { add(it) }
            logo?.url?.takeIf { it.isNotBlank() }?.let { add(it) }
        }
        val resolvedName = (name ?: slug).ifBlank { slug }
        val iconBytes = modFile?.let {
            runCatching { JarFile(it).use { jar -> jar.modLogo } }.getOrNull()
        }
        val introText = summary?.takeIf { it.isNotBlank() }?.trim()
            ?: modFile?.let { JarFile(it).readNeoForgeConfig()?.modDescription }
            ?: "暂无介绍"
        return ModBriefVo(
            name = resolvedName,
            nameCn = null,
            intro = introText,
            iconData = iconBytes,
            iconUrls = icons
        )
    }

    data class CurseForgeLocalCard(val slug: String, val brief: ModBriefVo)

    data class CurseForgeLocalResult(
        val cards: List<CurseForgeLocalCard> = emptyList(),
        val matchedFiles: Set<File> = emptySet(),
        val mods: List<Mod> = emptyList()
    )

    suspend fun discoverModsCurseForge(): CurseForgeLocalResult {
        return runCatching {
            if (mods.isEmpty()) {
                lgr.info("CurseForge: no local mods discovered for lookup")
                return@runCatching CurseForgeLocalResult()
            }

            val fingerprintData = getFingerprintsCurseForge()
            val fingerprintToFile = mods.associateBy { it.murmur2 }

            data class MatchRecord(val projectId: Long, val fileId: String, val fingerprint: String, val file: File)

            val matchRecords = fingerprintData.exactMatches.mapNotNull { match ->
                val projectId = match.id.takeIf { it > 0 } ?: return@mapNotNull null
                val fingerprint = match.file.fileFingerprint
                    ?: match.latestFiles.firstOrNull()?.fileFingerprint
                    ?: return@mapNotNull null
                val localFile = fingerprintToFile[fingerprint] ?: return@mapNotNull null
                MatchRecord(
                    projectId = projectId,
                    fileId = match.file.id.toString(),
                    fingerprint = fingerprint.toString(),
                    file = localFile
                )
            }

            val recordsByProject = matchRecords.groupBy { it.projectId }
            val modIds = recordsByProject.keys.toList()
            if (modIds.isEmpty()) {
                logUnmatched("CurseForge", mods)
                return@runCatching CurseForgeLocalResult()
            }

            val cfMods = getInfosCurseForge(modIds)

            data class CfModMeta(
                val projectId: Long,
                val canonicalSlug: String,
                val normalizedSlug: String,
                val files: List<MatchRecord>,
                val mod: CurseForgeMod
            )

            val cfModMeta = cfMods.mapNotNull { mod ->
                val projectId = mod.id ?: return@mapNotNull null
                val files = recordsByProject[projectId].orEmpty()
                if (files.isEmpty()) return@mapNotNull null
                val rawSlug = mod.slug.trim()
                val canonicalSlug = when {
                    rawSlug.isNotEmpty() -> rawSlug
                    !mod.name.isNullOrBlank() -> mod.name.trim()
                    else -> projectId.toString()
                }
                CfModMeta(
                    projectId = projectId,
                    canonicalSlug = canonicalSlug,
                    normalizedSlug = canonicalSlug.lowercase(),
                    files = files,
                    mod = mod
                )
            }

            val matchedFiles = cfModMeta.flatMap { meta -> meta.files.map { it.file } }.toSet()

            val discoveredMods = cfModMeta.flatMap { meta ->
                meta.files.map { record ->
                    Mod(
                        platform = "cf",
                        projectId = meta.projectId.toString(),
                        slug = meta.canonicalSlug,
                        fileId = record.fileId,
                        hash = record.fingerprint
                    )
                }
            }

            val cards = cfModMeta.map { meta ->
                val modFile = meta.files.first().file
                val brief = cfSlugBriefInfo[meta.normalizedSlug]?.toVo(modFile)
                    ?: meta.mod.toBriefVo(modFile)
                CurseForgeLocalCard(meta.normalizedSlug, brief)
            }

            val unmatched = mods.filterNot { it in matchedFiles }
            logUnmatched("CurseForge", unmatched)

            CurseForgeLocalResult(cards, matchedFiles, discoveredMods)
        }.onFailure {
            lgr.error("CurseForge: failed to fetch local mod metadata", it)
        }.getOrElse { CurseForgeLocalResult() }
    }

    suspend fun getVersionsModrinth(): Map<String, ModrinthVersionInfo> {

        if (mods.isEmpty()) {
            lgr.info("Modrinth lookup skipped: no mods found in ${MOD_DIR.absolutePath}")
            return emptyMap()
        }

        val hashes = mods.map { it.sha1 }
        val requestPayload = ModrinthVersionLookupRequest(hashes = hashes, algorithm = "sha1")

        val response = httpRequest {
            url("https://api.modrinth.com/v2/version_files")
            method = HttpMethod.Post
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
    fun readPCLModData() {

    }

}

