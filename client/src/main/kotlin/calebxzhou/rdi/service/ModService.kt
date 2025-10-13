package calebxzhou.rdi.service

import calebxzhou.rdi.auth.LocalCredentials.Companion.file
import calebxzhou.rdi.lgr
import calebxzhou.rdi.model.McmodInfo
import calebxzhou.rdi.model.ModAuthor
import calebxzhou.rdi.net.httpStringRequest
import calebxzhou.rdi.net.success
import calebxzhou.rdi.model.ModrinthVersionInfo
import calebxzhou.rdi.model.ModrinthVersionLookupRequest
import calebxzhou.rdi.net.body
import calebxzhou.rdi.service.ModService.MOD_DIR
import calebxzhou.rdi.service.ModService.mcmodHeader
import calebxzhou.rdi.service.ModService.mcmodSearchUrl
import calebxzhou.rdi.service.ModService.mods
import calebxzhou.rdi.util.json
import calebxzhou.rdi.util.serdesJson
import calebxzhou.rdi.util.sha1
import calebxzhou.rdi.util.urlEncoded
import com.electronwill.nightconfig.core.CommentedConfig
import com.electronwill.nightconfig.core.Config
import com.electronwill.nightconfig.toml.TomlFormat
import java.io.File
import java.net.URI
import java.util.jar.JarFile
import org.jsoup.Jsoup
import kotlin.collections.contains
import kotlin.collections.firstOrNull
import kotlin.collections.plusAssign


object ModService {
    const val NEOFORGE_CONFIG_PATH = "META-INF/neoforge.mods.toml"
    const val FABRIC_CONFIG_PATH = "fabric.mod.json"
    const val FORGE_CONFIG_PATH = "META-INF/mods.toml"

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
    val sha1Mods
        get() = mods.associateBy { it.sha1 }
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


    suspend fun getInfosModrinth(): Map<String, ModrinthVersionInfo> {

        if (mods.isEmpty()) {
            lgr.info("Modrinth lookup skipped: no mods found in ${MOD_DIR.absolutePath}")
            return emptyMap()
        }

        val hashes = mods.map { it.sha1 }
        val requestPayload = ModrinthVersionLookupRequest(hashes = hashes, algorithm = "sha1")

        val response = httpStringRequest(
            post = true,
            url = "https://api.modrinth.com/v2/version_files",
            jsonBody = requestPayload.json
        )

        if (!response.success) {
            lgr.warn("Modrinth lookup failed: HTTP ${response.statusCode()} ${response.body()}")
            return emptyMap()
        }

        val body = response.body()
        if (body.isNullOrBlank()) {
            lgr.warn("Modrinth lookup returned empty body")
            return emptyMap()
        }

        val decoded = runCatching {
            serdesJson.decodeFromString<Map<String, ModrinthVersionInfo>>(body)
        }.onFailure { err ->
            lgr.error("Failed to parse Modrinth response", err)
        }.getOrNull() ?: return emptyMap()

        val missing = hashes.filter { it !in decoded }
        if (missing.isNotEmpty()) {
            lgr.info("Modrinth: ${decoded.size} matches, ${missing.size} hashes unmatched")
        } else {
            lgr.info("Modrinth: matched all ${decoded.size} hashes")
        }

        return decoded
    }

    suspend fun getInfoMcmod(keyword: String): McmodInfo? {

        val modUrl = httpStringRequest(url = "${mcmodSearchUrl}${keyword.urlEncoded}", headers = mcmodHeader).body
            .let { Jsoup.parse(it) }
            .select(".result-item>.head>a").firstOrNull()
            ?.attr("href")
            ?: let {
                lgr.warn("mcmod未找到mod信息，关键词：$keyword")
                return null
            }
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
            lgr.warn("mcmod详情页请求失败: HTTP ${modResponse.statusCode()} url=$modUrl")
            return null
        }

        val modBody = modResponse.body()
        if (modBody.isNullOrBlank()) {
            lgr.warn("mcmod详情页返回空内容，url=$modUrl")
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
            name = resolvedName.ifBlank { keyword },
            nameCn = nameCn.ifBlank { resolvedName.ifBlank { keyword } },
            categories = categories,
            intro = intro,
            authors = authors
        )

    }

}

