package calebxzhou.rdi.service

import calebxzhou.mykotutils.std.sha1
import calebxzhou.rdi.model.ModBriefInfo
import calebxzhou.rdi.model.ModrinthProject
import calebxzhou.rdi.model.ModrinthVersionInfo
import calebxzhou.rdi.model.ModrinthVersionLookupRequest
import calebxzhou.rdi.net.httpRequest
import calebxzhou.rdi.net.json
import calebxzhou.rdi.service.ModService.briefInfo
import calebxzhou.rdi.util.Loggers
import calebxzhou.rdi.util.json
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.io.File

object ModrinthService {
    val slugBriefInfo: Map<String, ModBriefInfo> by lazy { ModService.buildSlugMap(briefInfo) { it.modrinthSlugs } }

    private val lgr by Loggers
    const val BASE_URL = "https://mod.mcimirror.top/modrinth/v2"
    const val OFFICIAL_URL = "https://api.modrinth.com/v2"

    //mr - cf slug, 没查到就返回自身
    val String.mr2CfSlug: String
        get() {
            if (isBlank()) return this
            val info = ModrinthService.slugBriefInfo[trim().lowercase()] ?: return this
            return info.curseforgeSlugs.firstOrNull { it.isNotBlank() }?.trim() ?: this
        }

    suspend fun mrreq(
        path: String,
        method: HttpMethod = HttpMethod.Get,
        params: Map<String, Any>? = null,
        body: Any? = null
    ): HttpResponse {
        suspend fun doRequest(base: String) = httpRequest {
            url("${base}/${path}")
            json()
            body?.let { setBody(it) }
            params?.forEach { parameter(it.key, it.value) }
            this.method = method
        }

        val mirrorResult = runCatching<HttpResponse> { doRequest(BASE_URL) }
        val mirrorResponse = mirrorResult.getOrNull()
        if (mirrorResponse != null && mirrorResponse.status.isSuccess()) {
            return mirrorResponse
        } else {
            lgr.warn("Modrinth镜像源请求失败，${mirrorResponse?.status},${mirrorResponse?.bodyAsText()}")
        }

        mirrorResult.exceptionOrNull()?.let {
            lgr.warn("Modrinth mirror request failed, falling back to official API: ${it.message}")
        }
        lgr.info("尝试使用官方api")
        val officialResponse = doRequest(OFFICIAL_URL)
        return officialResponse
    }

    //ID / slugs
    suspend fun List<String>.mapModrinthProjects(): List<ModrinthProject> {
        val normalizedIds = asSequence()
            .distinct()
            .toList()

        val chunkSize = 50
        val projects = mutableListOf<ModrinthProject>()

        normalizedIds.chunked(chunkSize).forEach { chunk ->
            val response = mrreq("projects", params = mapOf("ids" to chunk.json)).body<List<ModrinthProject>>()
            projects += response

            if (response.size != chunk.size) {
                val missing = chunk.toSet() - response.map { it.id }.toSet() - response.map { it.slug }.toSet()
                if (missing.isNotEmpty()) {
                    lgr.debug("Modrinth: ${missing.size} ids from chunk unmatched: ${missing.joinToString()}")
                }
            }
        }

        lgr.info("Modrinth: fetched ${projects.size} projects for ${normalizedIds.size} requested ids")

        return projects
    }

    suspend fun List<File>.mapModrinthVersions(): Map<String, ModrinthVersionInfo> {
        val hashes = map { it.sha1 }
        val response = mrreq(
            "version_files",
            method = HttpMethod.Post,
            body = ModrinthVersionLookupRequest(hashes = hashes, algorithm = "sha1")
        )
            .body<Map<String, ModrinthVersionInfo>>()

        val missing = hashes.filter { it !in response }
        if (missing.isNotEmpty()) {
            lgr.info("Modrinth: ${response.size} matches, ${missing.size} hashes unmatched")
        } else {
            lgr.info("Modrinth: matched all ${response.size} hashes")
        }

        return response
    }


}
