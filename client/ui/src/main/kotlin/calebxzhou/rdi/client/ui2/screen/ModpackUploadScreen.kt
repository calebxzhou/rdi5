package calebxzhou.rdi.client.ui2.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import calebxzhou.mykotutils.std.humanFileSize
import calebxzhou.mykotutils.std.humanSpeed
import calebxzhou.mykotutils.std.openChineseZip
import calebxzhou.mykotutils.std.urlEncoded
import calebxzhou.rdi.client.net.server
import calebxzhou.rdi.client.ui2.*
import calebxzhou.rdi.client.ui2.comp.ModCard
import calebxzhou.rdi.common.model.*
import calebxzhou.rdi.common.serdesJson
import calebxzhou.rdi.common.service.CurseForgeService
import calebxzhou.rdi.common.service.CurseForgeService.mapMods
import calebxzhou.rdi.common.service.ModrinthService
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.utils.io.streams.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.io.buffered
import org.bson.types.ObjectId
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * calebxzhou @ 2026-01-17 13:53
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModpackUploadScreen(
    onBack: () -> Unit,
    updateModpackId: ObjectId? = null,
    updateModpackName: String? = null
) {
    val scope = rememberCoroutineScope()
    var step: UploadStep by remember { mutableStateOf(UploadStep.Select) }
    var progressText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(step) {
        errorText = null
    }

    MainColumn {
        TitleRow(
            title = when (step) {
                is UploadStep.Select -> "上传整合包"
                is UploadStep.Confirm -> updateModpackName?.let { "为整合包$it 上传新版" } ?: "确认整合包信息"
                is UploadStep.Uploading -> "上传整合包"
                is UploadStep.Done -> "上传完成"
            },
            onBack = onBack
        ) {
            errorText?.let {
                Text(it, color = MaterialTheme.colors.error)
            }
            Space8w()
            when (step) {
                is UploadStep.Select -> {
                    CircleIconButton("\uF093", "选择文件") {
                        scope.launch(Dispatchers.IO) {
                            selectModpackFile(
                                onProgress = { progressText = it },
                                onError = { errorText = it; progressText = it },
                                onLoaded = { payload, mods ->
                                    val name = updateModpackName ?: payload.sourceName
                                    step = UploadStep.Confirm(payload, mods, name = name)
                                }
                            )
                        }
                    }
                }

                is UploadStep.Confirm -> {
                    CircleIconButton("\uF058", "确认上传") {
                        val current = step as UploadStep.Confirm
                        val name = current.name.trim()
                        val version = current.version.trim()
                        if (name.isBlank()) {
                            errorText = "整合包名称不能为空"
                            return@CircleIconButton
                        }
                        if (version.isBlank()) {
                            errorText = "版本号不能为空"
                            return@CircleIconButton
                        }
                        scope.launch(Dispatchers.IO) {
                            step = UploadStep.Uploading(current.payload, current.mods, name, version)
                            uploadModpack(
                                payload = current.payload,
                                mods = current.mods,
                                modpackName = name,
                                versionName = version,
                                updateModpackId = updateModpackId,
                                onProgress = { progressText = it },
                                onError = { errorText = it },
                                onDone = { summary ->
                                    step = UploadStep.Done(summary)
                                }
                            )
                        }
                    }
                }

                is UploadStep.Uploading -> {
                    // no actions while uploading
                }

                is UploadStep.Done -> {
                    CircleIconButton("\uF00C", "完成") {
                        onBack()
                    }
                }
            }
        }



        when (val current = step) {
            is UploadStep.Select -> {
                Space8h()
                Text("点击右上角按钮开始上传整合包。")
                if (progressText.isNotBlank()) {
                    Text(progressText)
                }
            }

            is UploadStep.Confirm -> {
                Row {

                    OutlinedTextField(
                        value = current.name,
                        onValueChange = { step = current.copy(name = it) },
                        modifier = 250.wM,
                        label = { Text("整合包名称") },
                        enabled = updateModpackName == null
                    )
                    OutlinedTextField(
                        value = current.version,
                        onValueChange = { step = current.copy(version = it) },
                        modifier = 120.wM,
                        label = { Text("版本") }
                    )
                }
                Text("mod列表：${current.mods.size}个")
                Space8h()
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(350.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(current.mods, key = { it.hash }) { mod ->
                        val card = mod.vo
                        if (card != null) {
                            card.ModCard()
                        } else {
                            Text(mod.slug)
                        }
                    }
                }
            }

            is UploadStep.Uploading -> {
                Text("正在上传整合包，请耐心等待...")
                Space8h()
                if (progressText.isNotBlank()) {
                    Text(progressText)
                }
            }

            is UploadStep.Done -> {
                Text(current.summary)
            }
        }
    }
}

private sealed class UploadStep {
    data object Select : UploadStep()
    data class Confirm(
        val payload: UploadPayload,
        val mods: List<Mod>,
        val name: String = payload.sourceName,
        val version: String = payload.sourceVersion
    ) : UploadStep()

    data class Uploading(
        val payload: UploadPayload,
        val mods: List<Mod>,
        val name: String,
        val version: String
    ) : UploadStep()

    data class Done(val summary: String) : UploadStep()
}

private suspend fun selectModpackFile(
    onProgress: (String) -> Unit,
    onError: (String) -> Unit,
    onLoaded: (UploadPayload, List<Mod>) -> Unit
) {
    val chooser = JFileChooser().apply {
        dialogTitle = "选择整合包 (ZIP / MRPACK)"
        fileSelectionMode = JFileChooser.FILES_ONLY
        currentDirectory = File("C:/Users/${System.getProperty("user.name")}/Downloads")
        fileFilter = FileNameExtensionFilter("整合包 (*.zip, *.mrpack)", "zip", "mrpack")
    }
    val result = chooser.showOpenDialog(null)
    if (result != JFileChooser.APPROVE_OPTION) return

    val file = chooser.selectedFile
    if (!file.exists() || !file.isFile) {
        onError("未找到所选文件")
        return
    }

    onProgress("已选择文件: ${file.name}。正在读取，请稍等几秒...")
    val packType = detectPackType(file)
    if (packType == PackType.MODRINTH) {
        val loaded = try {
            ModrinthService.loadModpack(file).getOrThrow()
        } catch (e: Exception) {
            e.printStackTrace()
            onError(e.message ?: "解析整合包失败")
            return
        }
        val mcVersion = loaded.mcVersion
        val modloader = loaded.modloader
        val versionName = loaded.index.versionId.ifBlank { "1.0" }
        val mods = loaded.mods
        onLoaded(
            UploadPayload(
                file = loaded.file,
                mods = mods,
                mcVersion = mcVersion,
                modloader = modloader,
                sourceName = loaded.index.name,
                sourceVersion = versionName
            ),
            mods
        )
        return
    }

    if (packType != PackType.CURSEFORGE) {
        onError("无效的整合包文件：缺少 manifest.json 或 modrinth.index.json")
        onProgress("无效的整合包文件：缺少 manifest.json 或 modrinth.index.json")
        return
    }
    val modpackData = try {
        CurseForgeService.loadModpack(file.absolutePath)
    } catch (e: Exception) {
        onError(e.message ?: "解析整合包失败")
        onProgress(e.message ?: "解析整合包失败")
        return
    }

    try {
        val mods = modpackData.manifest.files.mapMods()
        val mcVersion = McVersion.from(modpackData.manifest.minecraft.version)
        if (mcVersion == null) {
            onError("不支持的MC版本: ${modpackData.manifest.minecraft.version}")
            onProgress("不支持的MC版本: ${modpackData.manifest.minecraft.version}")
            return
        }
        val modloader = ModLoader.from(modpackData.manifest.minecraft.modLoaders.firstOrNull()?.id.orEmpty())
        if (modloader == null) {
            onError("不支持的Mod加载器: ${modpackData.manifest.minecraft.modLoaders.firstOrNull()?.id.orEmpty()}")
            onProgress("不支持的Mod加载器: ${modpackData.manifest.minecraft.modLoaders.firstOrNull()?.id.orEmpty()}")
            return
        }
        onLoaded(
            UploadPayload(
                file = modpackData.file,
                mods = mods,
                mcVersion = mcVersion,
                modloader = modloader,
                sourceName = modpackData.manifest.name,
                sourceVersion = modpackData.manifest.version.ifBlank { "1.0" }
            ),
            mods
        )
    } catch (e: Exception) {
        onError("解析整合包失败: ${e.message}")
        onProgress("解析整合包失败: ${e.message}")
    }
}

private suspend fun uploadModpack(
    payload: UploadPayload,
    mods: List<Mod>,
    modpackName: String,
    versionName: String,
    updateModpackId: ObjectId?,
    onProgress: (String) -> Unit,
    onError: (String) -> Unit,
    onDone: (String) -> Unit
) {
    val modpack = getOrCreateModpack(payload, modpackName, updateModpackId, onProgress, onError) ?: return

    if (modpack.versions.any { it.name.equals(versionName, ignoreCase = true) }) {
        onError("${modpack.name}包已经有$versionName 这个版本了")
        return
    }

    val modpackId = modpack._id.toHexString()
    val versionEncoded = versionName.urlEncoded

    onProgress("创建版本 ${versionName}...")

    val totalBytes = payload.file.length()
    val startTime = System.nanoTime()
    var lastProgressUpdate = 0L
    val modsJson = serdesJson.encodeToString(mods)
    val boundary = "rdi-modpack-${System.currentTimeMillis()}"
    val multipartContent = MultiPartFormDataContent(
        formData {
            append(
                key = "mods",
                value = modsJson,
                headers = Headers.build {
                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                }
            )
            append(
                key = "file",
                value = InputProvider { payload.file.inputStream().asInput().buffered() },
                headers = Headers.build {
                    append(HttpHeaders.ContentType, ContentType.Application.Zip.toString())
                    append(HttpHeaders.ContentDisposition, "filename=\"${payload.file.name}\"")
                }
            )
        },
        boundary = boundary
    )
    //install locally first

    val createVersionResp = server.makeRequest<Unit>(
        path = "modpack/$modpackId/version/$versionEncoded",
        method = HttpMethod.Post,
    ) {
        timeout {
            requestTimeoutMillis = 60 * 60 * 1000L
            socketTimeoutMillis = 60 * 60 * 1000L
        }
        setBody(multipartContent)
        onUpload { bytesSentTotal, contentLength ->
            val now = System.nanoTime()
            val shouldUpdate = contentLength != null && bytesSentTotal == contentLength ||
                    now - lastProgressUpdate > 75_000_000L
            if (shouldUpdate) {
                lastProgressUpdate = now
                val elapsedSeconds = (now - startTime) / 1_000_000_000.0
                val total = contentLength?.takeIf { it > 0 } ?: totalBytes
                val percent = if (total <= 0) 100 else ((bytesSentTotal * 100) / total).toInt()
                val speed = if (elapsedSeconds <= 0) 0.0 else bytesSentTotal / elapsedSeconds
                onProgress(
                    buildString {
                        appendLine("正在上传版本 ${versionName}...")
                        appendLine(
                            "进度：${
                                percent.coerceIn(
                                    0,
                                    100
                                )
                            }% (${bytesSentTotal.humanFileSize}/${total.humanFileSize})"
                        )
                        appendLine("速度：${speed.humanSpeed}")
                    }
                )
            }
        }
    }

    if (!createVersionResp.ok) {
        onError(createVersionResp.msg)
        return
    }

    val elapsedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0
    val speed = if (elapsedSeconds <= 0) 0.0 else totalBytes / elapsedSeconds
    onDone(
        buildString {
            appendLine("文件大小: ${totalBytes.humanFileSize}")
            appendLine("平均速度: ${speed.humanSpeed}")
            appendLine("耗时: ${"%.1f".format(elapsedSeconds)}秒")
            appendLine("传完了 服务器要开始构建 等5分钟 结果发你信箱里")
        }
    )
}

private suspend fun getOrCreateModpack(
    payload: UploadPayload,
    name: String,
    targetModpackId: ObjectId?,
    onProgress: (String) -> Unit,
    onError: (String) -> Unit
): Modpack? {
    onProgress(
        if (targetModpackId != null) {
            "获取整合包 ${name}..."
        } else {
            "检查整合包 ${name}..."
        }
    )
    val myModpacksResp = server.makeRequest<List<Modpack>>(
        path = "modpack/my",
        method = HttpMethod.Get
    )
    if (!myModpacksResp.ok) {
        onError(myModpacksResp.msg)
        return null
    }
    val myModpacks = myModpacksResp.data.orEmpty()

    if (targetModpackId != null) {
        val target = myModpacks.firstOrNull { it._id == targetModpackId }
        if (target == null) {
            onError("未找到要更新的整合包")
            return null
        }
        onProgress("为已有整合包 ${target.name} 上传新版")
        return target
    }

    val existing = myModpacks.firstOrNull { it.name.equals(name, ignoreCase = true) }
    if (existing != null) {
        onProgress("使用已有整合包 ${name}")
        return existing
    }

    try {
        validateModpackName(name)
    } catch (e: Exception) {
        onError(e.message ?: "整合包名称不合法")
        return null
    }
    onProgress("创建整合包 ${name}...")
    val mcVersion = payload.mcVersion
    val modloader = payload.modloader
    val createResp = server.makeRequest<Modpack>(
        path = "modpack",
        method = HttpMethod.Post,
        params = mapOf(
            "name" to name,
            "mcVer" to mcVersion,
            "modloader" to modloader
        )
    )
    if (!createResp.ok || createResp.data == null) {
        onError(createResp.msg)
        return null
    }
    return createResp.data
}

private data class UploadPayload(
    val file: File,
    val mods: List<Mod>,
    val mcVersion: McVersion,
    val modloader: ModLoader,
    val sourceName: String,
    val sourceVersion: String
)

private fun parseModrinthIds(downloads: List<String>): Pair<String, String>? {
    val url = downloads.firstOrNull { it.contains("modrinth.com/data/") } ?: return null
    val parts = url.substringAfter("modrinth.com/data/").split('/')
    val projectId = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return null
    val versionId = parts.getOrNull(2)?.takeIf { it.isNotBlank() } ?: return null
    return projectId to versionId
}

private enum class PackType { MODRINTH, CURSEFORGE, UNKNOWN }

private fun detectPackType(file: File): PackType {
    return runCatching {
        file.openChineseZip().use { zip ->
            var hasMrIndex = false
            var hasCfManifest = false
            val entries = zip.entries().asSequence()
            for (entry in entries) {
                if (entry.isDirectory) continue
                when (entry.name.substringAfterLast('/')) {
                    "modrinth.index.json" -> hasMrIndex = true
                    "manifest.json" -> hasCfManifest = true
                }
                if (hasMrIndex || hasCfManifest) break
            }
            when {
                hasMrIndex -> PackType.MODRINTH
                hasCfManifest -> PackType.CURSEFORGE
                else -> PackType.UNKNOWN
            }
        }
    }.getOrElse { PackType.UNKNOWN }
}
