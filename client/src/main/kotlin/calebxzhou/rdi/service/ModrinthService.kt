package calebxzhou.rdi.service

import calebxzhou.rdi.lgr
import calebxzhou.rdi.model.ModrinthProject
import calebxzhou.rdi.model.ModrinthVersionInfo
import calebxzhou.rdi.model.ModrinthVersionLookupRequest
import calebxzhou.rdi.net.httpRequest
import calebxzhou.rdi.net.json
import calebxzhou.rdi.util.serdesJson
import calebxzhou.rdi.util.sha1
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import java.io.File

object ModrinthService {
    suspend fun getVersions(mods: List<File>): Map<String, ModrinthVersionInfo> {

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

    suspend fun getProjects(ids: List<String>): List<ModrinthProject> {

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
}