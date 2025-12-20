package calebxzhou.rdi.master.service

import calebxzhou.mykotutils.ktor.downloadFileFrom
import calebxzhou.mykotutils.log.Loggers
import calebxzhou.mykotutils.std.humanSpeed
import calebxzhou.mykotutils.std.murmur2
import calebxzhou.mykotutils.std.toFixed
import calebxzhou.rdi.master.CONF
import calebxzhou.rdi.master.DOWNLOAD_MODS_DIR
import calebxzhou.rdi.master.exception.RequestError
import calebxzhou.rdi.master.model.CurseForgeFile
import calebxzhou.rdi.master.model.CurseForgeFileResponse
import calebxzhou.rdi.master.model.pack.Mod
import calebxzhou.rdi.master.net.httpRequest
import calebxzhou.rdi.master.net.json
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.nio.file.Path
import kotlin.math.max
import kotlin.math.min

object CurseForgeService {
    private val lgr by Loggers
    const val BASE_URL = "https://api.curseforge.com/v1"
    suspend inline fun cfreq(path: String, method: HttpMethod = HttpMethod.Get, body: Any? = null) =
        httpRequest {
            url("${BASE_URL}/${path}")
            json()
            header("x-api-key", CONF.apiKey.curseforge)
            body?.let { setBody(it) }
            this.method = method
        }
    suspend fun getModFileInfo(modId: String, fileId: String): CurseForgeFile? {
        return cfreq("mods/${modId}/files/${fileId}").body<CurseForgeFileResponse>().data
    }
    private val MAX_PARALLEL_DOWNLOADS = max(4, Runtime.getRuntime().availableProcessors() * 4)


    suspend fun downloadMod(mod: Mod,onProgress: (String) -> Unit = {}): Path {
        val targetFile = File(DOWNLOAD_MODS_DIR, mod.fileName)
        if (targetFile.exists()
            && targetFile.murmur2.toString() == mod.hash
            && mod.platform == "cf"
        ) {
            lgr.info { "mod已存在，跳过下载： ${mod.slug}" }
            return targetFile.toPath()
        }
        val downloadUrl = getModFileInfo(mod.projectId,mod.fileId)?.realDownloadUrl
        if (downloadUrl.isNullOrBlank()) {
            throw RequestError("无法获取下载链接: ${mod.slug}")
        }
        /*downloadUrl = downloadUrl.replace("edge.forgecdn.net", "mod.mcimirror.top").
        replace("mediafilez.forgecdn.net", "mod.mcimirror.top").
        replace("media.forgecdn.net", "mod.mcimirror.top")*/
        val success = targetFile.toPath().downloadFileFrom(downloadUrl) { progress ->
            "mod下载中： ${mod.slug} ${progress.percent.toFixed(2)}% ${progress.speedBytesPerSecond.humanSpeed}".run {
                lgr.info { this }
                onProgress(this)
            }
        }

        if (!success) {
            "下载mod失败: ${mod.slug}".run { onProgress(this);lgr.error { this } }
        }
        lgr.info { "下载完成：$mod" }
        return targetFile.toPath()
    }

    suspend fun downloadMods(mods: List<Mod>,onProgress: (String) -> Unit = {}): List<Path> {
        if (mods.isEmpty()) return emptyList()
        val parallelism = min(MAX_PARALLEL_DOWNLOADS, mods.size)
        onProgress("下载${mods.size}个Mod：${mods.map { it.slug }}")
        lgr.info { "下载${mods.size}个mod，最大并发：$parallelism" }
        val semaphore = Semaphore(parallelism)
        val downloaded = mutableListOf<Path>()
        val failures = mutableListOf<Pair<Mod, Throwable>>()

        coroutineScope {
            val outcomes = mods.map { mod ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        mod to runCatching {
                            downloadMod(mod)
                        }
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