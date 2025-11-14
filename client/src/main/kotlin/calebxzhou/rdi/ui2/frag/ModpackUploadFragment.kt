package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.Const
import calebxzhou.rdi.exception.ModpackException
import calebxzhou.rdi.model.CurseForgeModpackData
import calebxzhou.rdi.model.CurseForgePackManifest
import calebxzhou.rdi.model.pack.Mod
import calebxzhou.rdi.model.pack.Modpack
import calebxzhou.rdi.net.formatSpeed
import calebxzhou.rdi.net.humanSize
import calebxzhou.rdi.net.server
import calebxzhou.rdi.service.CurseForgeService
import calebxzhou.rdi.service.CurseForgeService.fillCurseForgeVo
import calebxzhou.rdi.service.CurseForgeService.toMods
import calebxzhou.rdi.ui2.FragmentSize
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.button
import calebxzhou.rdi.ui2.component.ModGrid
import calebxzhou.rdi.ui2.component.RTextField
import calebxzhou.rdi.ui2.component.alertErr
import calebxzhou.rdi.ui2.editText
import calebxzhou.rdi.ui2.go
import calebxzhou.rdi.ui2.linearLayout
import calebxzhou.rdi.ui2.plusAssign
import calebxzhou.rdi.ui2.pointerBuffer
import calebxzhou.rdi.ui2.textField
import calebxzhou.rdi.ui2.textView
import calebxzhou.rdi.ui2.uiThread
import calebxzhou.rdi.util.ioTask
import calebxzhou.rdi.util.urlEncoded
import icyllis.modernui.widget.TextView
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
import org.lwjgl.PointerBuffer
import org.lwjgl.util.tinyfd.TinyFileDialogs
import java.io.File
import kotlin.math.min

class ModpackUploadFragment : RFragment("上传整合包") {
    lateinit var progressEditText: TextView
    var progressText
        get() = progressEditText.text.toString()
        set(value) = uiThread { progressEditText.text = value }
    override var fragSize: FragmentSize
        get() = FragmentSize.SMALL
        set(value) {}
    init {
        contentViewInit = {
            textView("上传现成的整合包 大家一起玩 ")
            textView("目前仅支持1.21.1 NeoForge的包")
            button("选择文件") { selectModpackFile() }
            progressEditText = textView("")
        }
    }
    private fun selectModpackFile() = ioTask {
        val initialPath = if (Const.DEBUG) "C:/Users/${System.getProperty("user.name")}/Downloads" else null
        val selected = TinyFileDialogs.tinyfd_openFileDialog(
            "选择整合包 (ZIP)",
            initialPath,
            ("*.zip").pointerBuffer,
            "CurseForge整合包 (*.zip)",
            false
        ) ?: return@ioTask

        val file = File(selected)
        if (!file.exists() || !file.isFile) {
            alertErr("未找到所选文件")
            progressText = "未找到所选文件"
            return@ioTask
        }

        progressText = "已选择文件: ${file.name}"
        loadModpack(file.absolutePath)
    }

    class Confirm(var data: CurseForgeModpackData,val mods: List<Mod>) : RFragment("确认整合包信息") {
        lateinit var nameEdit : RTextField
        lateinit var verEdit : RTextField

        init {
            contentViewInit = {
                val manifest = data.manifest
                linearLayout {
                    nameEdit=textField ("整合包名称") { edit.setText(manifest.name) }
                    verEdit=textField ("版本") { edit.setText(manifest.version) }
                }
                textView("mod列表：")
                this += ModGrid(context,mods=mods)
            }
            titleViewInit = {
                textView("${mods.size}个Mod ${data.zip.size()}个文件 共${data.file.length().humanSize}")

                quickOptions {
                    "确认上传" colored MaterialColor.GREEN_900 with {
                        data.manifest.name=nameEdit.text.toString()
                        data.manifest.version=verEdit.text.toString()
                        Upload(data,mods).go(false)
                    }
                }
            }
        }
    }
    class Upload(val data: CurseForgeModpackData, var mods: List<Mod>) : RFragment("上传整合包") {
        override var closable = false
        lateinit var progressEditText: TextView
        override var preserveViewStateOnDetach: Boolean
            get() = false
            set(value) {}
        override var fragSize: FragmentSize
            get() = FragmentSize.SMALL
            set(value) {}
        var progressText
            get() = progressEditText.text.toString()
            set(value) = uiThread { progressEditText.setText(value) }

        init {
            contentViewInit = {
                textView("正在上传整合包，请耐心等待...")
                progressEditText = textView("")
                upload()
            }
        }


        private fun upload() = ioTask {
            try {
                val manifest = data.manifest

                progressText = "创建整合包中..."
                val createResp = server.makeRequest<Modpack>(
                    path = "modpack/",
                    method = HttpMethod.Post,
                    params = mapOf("name" to manifest.name)
                )
                if (!createResp.ok || createResp.data == null) {
                    alertErr(createResp.msg)
                    progressText = createResp.msg
                    closable = true
                    return@ioTask
                }
                val modpack = createResp.data
                val modpackId = modpack._id.toHexString()

                val versionEncoded = manifest.version.urlEncoded
                progressText = "创建版本 ${manifest.version}..."

                // Read the entire zip file
                val zipBytes = data.file.readBytes()
                val totalBytes = zipBytes.size.toLong()

                val startTime = System.nanoTime()
                var lastProgressUpdate = 0L
                val progressContent = UploadProgressContent(zipBytes) { sentBytes, total ->
                    val now = System.nanoTime()
                    if (sentBytes == total || now - lastProgressUpdate > 75_000_000L) {
                        lastProgressUpdate = now
                        val elapsedSeconds = (now - startTime) / 1_000_000_000.0
                        val percent = if (total == 0L) 100 else ((sentBytes * 100) / total).toInt()
                        val speed = if (elapsedSeconds <= 0) 0.0 else sentBytes / elapsedSeconds
                        progressText = buildString {
                            appendLine("正在上传版本 ${manifest.version}...")
                            appendLine("进度：${percent.coerceIn(0, 100)}% (${sentBytes.humanSize}/${total.humanSize})")
                            appendLine("速度：${formatSpeed(speed)}")
                        }
                    }
                }

                val createVersionResp = server.makeRequest<Unit>(
                    path = "modpack/$modpackId/version/$versionEncoded",
                    method = HttpMethod.Post,
                ) {
                    setBody(progressContent)
                }
                
                if (!createVersionResp.ok) {
                    alertErr(createVersionResp.msg)
                    progressText = createVersionResp.msg
                    closable = true
                    return@ioTask
                }
                
                val elapsedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0
                val speed = if (elapsedSeconds <= 0) 0.0 else totalBytes / elapsedSeconds

                progressText = buildString {
                    appendLine("上传完成")
                    appendLine("文件大小: ${totalBytes.humanSize}")
                    appendLine("平均速度: ${formatSpeed(speed)}")
                    appendLine("耗时: ${"%.1f".format(elapsedSeconds)}秒")
                    appendLine("整合包上传完成")
                }
                contentView.apply { button("完成", MaterialColor.GREEN_900){ ModpackUploadFragment().go(false) } }
                closable = true
            } finally {
                data.close()
            }
        }
    }

    private fun loadModpack(zipPath: String) = ioTask {
        closable = false
        progressText = "正在解析整合包..."

        val modpackData = try {
            CurseForgeService.loadModpack(zipPath)
        } catch (e: ModpackException) {
            alertErr(e.message ?: "未知错误")
            progressText = e.message ?: "未知错误"
            closable = true
            return@ioTask
        }

        try {
            val manifest = modpackData.manifest
            progressText = "读取到${manifest.files.size}个mod 导入整合包中..."
            var mods = manifest.files.toMods()
            mods=mods.fillCurseForgeVo()
            Confirm(modpackData, mods).go()
        } catch (e: Exception) {
            alertErr("解析整合包失败: ${e.message}")
            progressText = "解析整合包失败: ${e.message}"
            modpackData.close()
            closable = true
        }
    }
}

private class UploadProgressContent(
    private val payload: ByteArray,
    private val chunkSize: Int = DEFAULT_CHUNK_SIZE,
    private val onProgress: (sent: Long, total: Long) -> Unit,
) : OutgoingContent.WriteChannelContent() {
    override val contentLength: Long = payload.size.toLong()
    override val contentType: ContentType = ContentType.Application.Zip

    override suspend fun writeTo(channel: ByteWriteChannel) {
        if (payload.isEmpty()) {
            onProgress(0, 0)
            return
        }
        var offset = 0
        val total = payload.size
            while (offset < total) {
                val length = min(chunkSize, total - offset)
                channel.writeFully(payload, offset, offset + length)
                offset += length
            onProgress(offset.toLong(), total.toLong())
        }
    }

    private companion object {
        private const val DEFAULT_CHUNK_SIZE = 256 * 1024
    }
}
