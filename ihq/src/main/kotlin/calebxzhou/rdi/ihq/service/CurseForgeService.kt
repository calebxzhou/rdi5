package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.CONF
import calebxzhou.rdi.ihq.DOWNLOAD_MODS_DIR
import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.lgr
import calebxzhou.rdi.ihq.model.pack.Mod
import calebxzhou.rdi.ihq.net.downloadFileWithProgress
import calebxzhou.rdi.ihq.net.ktorClient
import calebxzhou.rdi.ihq.util.murmur2
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable
import java.io.File
import java.nio.file.Path
import kotlin.math.max
import kotlin.math.min

object CurseForgeService {
    const val BASE_URL = "https://mod.mcimirror.top/curseforge/v1"
    private val MAX_PARALLEL_DOWNLOADS = max(4, Runtime.getRuntime().availableProcessors() * 2)
    @Serializable
    data class DataResponse(val data: String)

    suspend fun downloadMod(mod: Mod): Path {
        val targetFile = File(DOWNLOAD_MODS_DIR, mod.fileName)
        if (targetFile.exists()
            && targetFile.murmur2.toString() == mod.hash
            && mod.platform == "cf"
        ) {
            lgr.info { "mod已存在，跳过下载： ${mod.slug}" }
            return targetFile.toPath()
        }
        val apiKey = CONF.apiKey.curseforge
        DOWNLOAD_MODS_DIR.mkdirs()
        var downloadUrl = ktorClient.get("${BASE_URL}/mods/${mod.projectId}/files/${mod.fileId}/download-url") {
            if (apiKey.isNotBlank()) {
                header("x-api-key", apiKey)
            }
        }.body<DataResponse>().data

        if (downloadUrl.isBlank()) {
            throw RequestError("无法获取下载链接: ${mod.slug}")
        }
        /*downloadUrl = downloadUrl.replace("edge.forgecdn.net", "mod.mcimirror.top").
        replace("mediafilez.forgecdn.net", "mod.mcimirror.top").
        replace("media.forgecdn.net", "mod.mcimirror.top")*/
        val success = downloadFileWithProgress(downloadUrl, targetFile.toPath()) { progress ->
            lgr.info { "mod下载中： ${mod.slug} ${progress.percent}%" }
        }

        if (!success) {
            throw RequestError("下载mod失败: ${mod.slug}")
        }
        lgr.info { "下载完成：$mod" }
        return targetFile.toPath()
    }

    suspend fun downloadMods(mods: List<Mod>): List<Path> {
        if (mods.isEmpty()) return emptyList()
        val parallelism = min(MAX_PARALLEL_DOWNLOADS, mods.size)
        lgr.info { "下载${mods.size}个mod，最大并发：$parallelism" }
        val semaphore = Semaphore(parallelism)
        val downloaded = mutableListOf<Path>()
        val failures = mutableListOf<Pair<Mod, Throwable>>()

        coroutineScope {
            val outcomes = mods.map { mod ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        mod to runCatching { downloadMod(mod) }
                    }
                }
            }.awaitAll()

            outcomes.forEach { (mod, result) ->
                result
                    .onSuccess { downloaded.add(it) }
                    .onFailure { error ->
                        failures += mod to error
                        lgr.error(error) { "下载mod失败: ${mod.slug}" }
                    }
            }
        }

        if (failures.isNotEmpty()) {
            val failedNames = failures.joinToString { it.first.slug }
            throw RequestError("部分mod下载失败: $failedNames")
        }
        return downloaded
    }
}