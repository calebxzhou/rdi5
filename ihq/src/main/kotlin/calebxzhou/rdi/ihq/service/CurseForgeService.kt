package calebxzhou.rdi.ihq.service

import calebxzhou.rdi.ihq.CONF
import calebxzhou.rdi.ihq.DOWNLOAD_MODS_DIR
import calebxzhou.rdi.ihq.exception.RequestError
import calebxzhou.rdi.ihq.lgr
import calebxzhou.rdi.ihq.model.CurseForgeFile
import calebxzhou.rdi.ihq.model.CurseForgeFileResponse
import calebxzhou.rdi.ihq.model.pack.Mod
import calebxzhou.rdi.ihq.net.downloadFileWithProgress
import calebxzhou.rdi.ihq.net.httpRequest
import calebxzhou.rdi.ihq.net.json
import calebxzhou.rdi.ihq.net.ktorClient
import calebxzhou.rdi.ihq.util.murmur2
import com.sun.org.apache.bcel.internal.Const
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.Serializable
import java.io.File
import java.nio.file.Path
import javax.ws.rs.client.Entity.json
import kotlin.math.max
import kotlin.math.min

object CurseForgeService {
    //const val BASE_URL = "https://mod.mcimirror.top/curseforge/v1"
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


    suspend fun downloadMod(mod: Mod): Path {
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