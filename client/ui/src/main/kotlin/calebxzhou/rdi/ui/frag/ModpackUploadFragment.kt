package calebxzhou.rdi.ui.frag

import calebxzhou.mykotutils.std.humanFileSize
import calebxzhou.mykotutils.std.humanSpeed
import calebxzhou.mykotutils.std.urlEncoded
import calebxzhou.rdi.common.model.CurseForgeModpackData
import calebxzhou.rdi.common.model.Mod
import calebxzhou.rdi.common.model.Modpack
import calebxzhou.rdi.common.serdesJson
import calebxzhou.rdi.common.service.CurseForgeService
import calebxzhou.rdi.common.service.CurseForgeService.mapMods
import calebxzhou.rdi.common.util.ioTask
import calebxzhou.rdi.net.server
import calebxzhou.rdi.ui.*
import calebxzhou.rdi.ui.component.ModGrid
import calebxzhou.rdi.ui.component.RTextField
import calebxzhou.rdi.ui.component.alertErr
import icyllis.modernui.view.View
import icyllis.modernui.widget.TextView
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.utils.io.streams.*
import kotlinx.io.buffered
import org.bson.types.ObjectId
import org.lwjgl.util.tinyfd.TinyFileDialogs
import java.io.File
import kotlin.collections.orEmpty

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
            textView("上传现成的整合包 大家一起玩！")
            button("选择文件") { selectModpackFile() }
            progressEditText = textView("")
        }
    }

    companion object {

    }

    private fun selectModpackFile() = ioTask {
        val initialPath = "C:/Users/${System.getProperty("user.name")}/Downloads"
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

    class Confirm(
        var data: CurseForgeModpackData, val mods: List<Mod>,
        val updateModpackId: ObjectId? = null,
        val updateModpackName: String? = null,
    ) : RFragment(updateModpackName?.let { "为整合包${updateModpackName}上传新版" } ?: "确认整合包信息") {
        lateinit var nameEdit: RTextField
        lateinit var verEdit: RTextField

        init {
            contentViewInit = {
                val manifest = data.manifest
                linearLayout {
                    nameEdit = textField("整合包名称") {
                        edit.setText(updateModpackName ?: manifest.name)

                    }
                    verEdit = textField("版本") { edit.setText(manifest.version) }
                    //如果是升级 不允许改名称
                    if (updateModpackName != null) nameEdit.visibility = View.GONE
                }
                textView("mod列表：${mods.size}个")
                this += ModGrid(context, mods = mods)
            }
            titleViewInit = {
                textView("${mods.size}个Mod ${data.zip.size()}个文件 共${data.file.length().humanFileSize}")

                quickOptions {
                    "确认上传" colored MaterialColor.GREEN_900 with {
                        data.manifest.name = nameEdit.text
                        data.manifest.version = verEdit.text
                        Upload(data, mods, updateModpackId).go()
                    }
                }
            }
        }

        override fun close() {
            data.close()
            super.close()
        }
    }

    class Upload(val data: CurseForgeModpackData, var mods: List<Mod>, val updateModpackId: ObjectId?) :
        RFragment("上传整合包") {
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
            set(value) = uiThread { progressEditText.text = value }

        init {
            contentViewInit = {
                textView("正在上传整合包，请耐心等待...")
                progressEditText = textView("")
                upload()
            }
        }


        private fun upload() = ioTask {

            val manifest = data.manifest
            val modpackName = manifest.name.trim()
            val versionName = manifest.version.trim()
            if (modpackName.isEmpty()) {
                close()
                alertErr("整合包名称不能为空")
                return@ioTask
            }
            if (versionName.isEmpty()) {
                close()
                alertErr("版本号不能为空")
                return@ioTask
            }
            manifest.name = modpackName
            manifest.version = versionName

            val modpack = getOrCreateModpack(modpackName, updateModpackId) ?: run {
                return@ioTask
            }

            if (modpack.versions.any { it.name.equals(versionName, ignoreCase = true) }) {
                val msg = "${modpack.name}包已经有$versionName 这个版本了"
                close()
                alertErr(msg)
                return@ioTask
            }

            val modpackId = modpack._id.toHexString()

            val versionEncoded = versionName.urlEncoded
            progressText = "创建版本 ${versionName}..."

            val totalBytes = data.file.length()

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
                        value = InputProvider { data.file.inputStream().asInput().buffered() },
                        headers = Headers.build {
                            append(HttpHeaders.ContentType, ContentType.Application.Zip.toString())
                            append(HttpHeaders.ContentDisposition, "filename=\"${data.file.name}\"")
                        }
                    )
                },
                boundary = boundary
            )

            val createVersionResp = server.makeRequest<Unit>(
                path = "modpack/$modpackId/version/$versionEncoded",
                method = HttpMethod.Post,
            ) {
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
                        progressText = buildString {
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
                    }
                }
            }

            if (!createVersionResp.ok) {
                close()
                alertErr(createVersionResp.msg)
                return@ioTask
            }

            val elapsedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0
            val speed = if (elapsedSeconds <= 0) 0.0 else totalBytes / elapsedSeconds

            progressText = buildString {
                appendLine("文件大小: ${totalBytes.humanFileSize}")
                appendLine("平均速度: ${speed.humanSpeed}")
                appendLine("耗时: ${"%.1f".format(elapsedSeconds)}秒")
                appendLine("传完了 服务器要开始构建 等5分钟 结果发你信箱里")
            }
            uiThread {
                closable = true
                contentView.apply { button("完成", MaterialColor.GREEN_900) { close() } }
            }

        }

        private suspend fun getOrCreateModpack(name: String, targetModpackId: ObjectId?): Modpack? {
            progressText = if (targetModpackId != null) {
                "获取整合包 ${name}..."
            } else {
                "检查整合包 ${name}..."
            }
            val myModpacksResp = server.makeRequest<List<Modpack>>(
                path = "modpack/my",
                method = HttpMethod.Get
            )
            if (!myModpacksResp.ok) {
                close()
                alertErr(myModpacksResp.msg)
                return null
            }
            val myModpacks = myModpacksResp.data.orEmpty()

            if (targetModpackId != null) {
                val target = myModpacks.firstOrNull { it._id == targetModpackId }
                if (target == null) {
                    val msg = "未找到要更新的整合包"
                    close()
                    alertErr(msg)
                    return null
                }
                progressText = "为已有整合包 ${target.name} 上传新版"
                return target
            }

            val existing = myModpacks.firstOrNull { it.name.equals(name, ignoreCase = true) }
            if (existing != null) {
                progressText = "使用已有整合包 ${name}"
                return existing
            }

            progressText = "创建整合包 ${name}..."
            val createResp = server.makeRequest<Modpack>(
                path = "modpack",
                method = HttpMethod.Post,
                params = mapOf("name" to name)
            )
            if (!createResp.ok || createResp.data == null) {
                close()
                alertErr(createResp.msg)
                return null
            }
            return createResp.data
        }
    }

    private fun loadModpack(zipPath: String) = ioTask {
        closable = false
        progressText = "正在解析整合包..."

        val modpackData = try {
            CurseForgeService.loadModpack(zipPath)
        } catch (e: Exception) {
            alertErr(e.message ?: "未知错误")
            progressText = e.message ?: "未知错误"
            closable = true
            return@ioTask
        }

        try {
            val manifest = modpackData.manifest
            progressText = "读取到${manifest.files.size}个mod 导入整合包中..."
            val mods = manifest.files.mapMods()
            Confirm(modpackData, mods).go()
        } catch (e: Exception) {
            alertErr("解析整合包失败: ${e.message}")
            progressText = "解析整合包失败: ${e.message}"
            modpackData.close()
            closable = true
        }
    }
}
