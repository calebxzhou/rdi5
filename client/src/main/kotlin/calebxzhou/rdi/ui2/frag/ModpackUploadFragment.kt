package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.exception.ModpackException
import calebxzhou.rdi.model.pack.Modpack
import calebxzhou.rdi.net.formatSpeed
import calebxzhou.rdi.net.humanSize
import calebxzhou.rdi.net.server
import calebxzhou.rdi.service.CurseForgeService
import calebxzhou.rdi.ui2.button
import calebxzhou.rdi.ui2.component.alertErr
import calebxzhou.rdi.ui2.editText
import calebxzhou.rdi.ui2.textView
import calebxzhou.rdi.ui2.uiThread
import calebxzhou.rdi.util.ioTask
import calebxzhou.rdi.util.urlEncoded
import icyllis.modernui.widget.EditText
import io.ktor.client.request.*
import io.ktor.http.*
import java.util.*
import kotlin.math.max

class ModpackUploadFragment : RFragment("上传整合包") {
    lateinit var packPathEdit: EditText
    lateinit var progressText: EditText

    init {
        contentViewInit = {
            textView("你可以选择现成的整合包，上传到rdi服务器上，所有的玩家都可以玩。")
            textView("目前仅支持1.21.1 NeoForge的包")
            textView("要求：zip格式，并且由overrides和manifest.json文件组成（CurseForge格式的包）")
            textView("在下方输入整合包文件路径，如 D:\\mc\\downloads\\ftbskies2.zip")
            packPathEdit = editText("整合包路径")
            button("下一步") { loadModpack() }
            progressText = editText("")
        }
    }

    private fun loadModpack() {
        val zipPath = packPathEdit.text?.toString()?.trim().orEmpty()
        ioTask {
            uiThread {
                closable = false
                progressText.setText("正在解析整合包...")
            }

            val modpackData = try {
                CurseForgeService.loadModpack(zipPath)
            } catch (e: ModpackException) {
                alertErr(e.message ?: "未知错误")
                uiThread {
                    progressText.setText(e.message ?: "未知错误")
                    closable = true
                }
                return@ioTask
            }

            try {
                val manifest = modpackData.manifest

                uiThread { progressText.setText("创建整合包中...") }
                val createResp = server.makeRequest<Modpack>(
                    path = "modpack/",
                    method = HttpMethod.Post,
                    params = mapOf("name" to manifest.name)
                )
                if (!createResp.ok || createResp.data == null) {
                    alertErr(createResp.msg)
                    uiThread {
                        progressText.setText(createResp.msg)
                        closable = true
                    }
                    return@ioTask
                }
                val modpack = createResp.data
                val modpackId = modpack._id.toHexString()

                val versionEncoded = manifest.version.urlEncoded
                uiThread { progressText.setText("创建版本 ${manifest.version}...") }
                val createVersionResp = server.makeRequest<Unit>(
                    path = "modpack/$modpackId/$versionEncoded",
                    method = HttpMethod.Post
                )
                if (!createVersionResp.ok) {
                    alertErr(createVersionResp.msg)
                    uiThread {
                        progressText.setText(createVersionResp.msg)
                        closable = true
                    }
                    return@ioTask
                }

                var totalBytes = modpackData.overrideEntries.sumOf { entry -> max(0L, entry.size) }
                var uploadedBytes = 0L
                val startTime = System.nanoTime()

                modpackData.overrideEntries.forEachIndexed { index, entry ->
                    val relativePath = entry.name.removePrefix(modpackData.overridesFolder)
                    if (relativePath.isEmpty()) return@forEachIndexed

                    val data = modpackData.zip.getInputStream(entry).use { it.readBytes() }
                    if (entry.size < 0) {
                        totalBytes += data.size
                    }

                    val encodedPath = relativePath.split('/')
                        .filter { it.isNotEmpty() }
                        .joinToString("/") { it.urlEncoded }

                    val uploadResp = server.makeRequest<Unit>(
                        path = "modpack/$modpackId/$versionEncoded/file/$encodedPath",
                        method = HttpMethod.Post,
                    ) {
                        contentType(ContentType.Application.OctetStream)
                        setBody(data)
                    }

                    if (!uploadResp.ok) {
                        alertErr("上传 ${relativePath} 失败: ${uploadResp.msg}")
                        uiThread {
                            progressText.setText("上传 ${relativePath} 失败: ${uploadResp.msg}")
                            closable = true
                        }
                        return@ioTask
                    }

                    uploadedBytes += data.size
                    val elapsedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0
                    val progress = if (totalBytes == 0L) 1.0 else uploadedBytes.toDouble() / totalBytes
                    val progressPercent = progress.coerceIn(0.0, 1.0) * 100.0
                    val speed = if (elapsedSeconds <= 0) 0.0 else uploadedBytes / elapsedSeconds

                    uiThread {
                        progressText.setText(
                            buildString {
                                appendLine("上传进度 ${index + 1}/${modpackData.overrideEntries.size}")
                                appendLine(relativePath)
                                appendLine(
                                    "${uploadedBytes.toLong().humanSize}/${totalBytes.toLong().humanSize} " +
                                            "(${String.format(Locale.CHINA, "%.1f%%", progressPercent)}) " +
                                            formatSpeed(speed)
                                )
                            }
                        )
                    }
                }

                uiThread {
                    progressText.setText("整合包上传完成，可以退出此界面了")
                    closable = true
                }
            } finally {
                modpackData.close()
            }
        }
    }
}
