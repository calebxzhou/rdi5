package calebxzhou.rdi.ui.frag

import calebxzhou.mykotutils.ktor.downloadFileFrom
import calebxzhou.mykotutils.std.humanFileSize
import calebxzhou.mykotutils.std.sha1
import calebxzhou.rdi.common.DL_MOD_DIR
import calebxzhou.rdi.common.exception.RequestError
import calebxzhou.rdi.common.model.McVersion
import calebxzhou.rdi.common.util.ioTask
import calebxzhou.rdi.lgr
import calebxzhou.rdi.net.server
import calebxzhou.rdi.ui.*
import calebxzhou.rdi.ui.component.RButton
import icyllis.modernui.widget.TextView
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.system.exitProcess

class UpdateFragment() : RFragment("正在检查更新") {

    private val uiJarFile: File = System.getProperty("rdi.jar.ui")?.let { File(it) }?:File("rdi-5-ui.jar").normalize().absoluteFile
    private var retryButtonAdded = false
    override var fragSize= FragmentSize.SMALL
    override var closable = false
    lateinit var progressText: TextView
    init {

        titleViewInit = {
            quickOptions {
                "> 跳过更新" with { LoginFragment().go() }
            }
        }
        contentViewInit = {
            progressText = textView {  }
            startUpdateFlow()
        }
    }
    fun logInfo(msg:String)=uiThread {
        progressText.text = msg
    }
    private fun startUpdateFlow()=ioTask {
        runCatching {
            val mcTargets = McVersion.entries.flatMap { mcVer ->
                mcVer.loaderVersions.keys.map { loader ->
                    val slug = "${mcVer.mcVer}-${loader.name.lowercase()}"
                    slug to DL_MOD_DIR.resolve("rdi-5-mc-client-$slug.jar").absoluteFile
                }
            }

            mcTargets.forEach { (slug, mcFile) ->
                val mcHash = server.makeRequest<String>("update/mc/$slug/hash").data
                    ?: throw RequestError("获取MC核心版本信息失败: $slug")

                val mcNeedsUpdate = !mcFile.exists() || mcFile.sha1 != mcHash
                if (mcNeedsUpdate) {
                    logInfo("准备下载 ${mcFile.name}...")
                    val mcUpdated = downloadAndReplaceCore(
                        targetFile = mcFile,
                        downloadUrl = "${server.hqUrl}/update/mc/$slug",
                        expectedSha = mcHash,
                        label = mcFile.name
                    )
                    if (!mcUpdated) {
                        showRetryOption()
                        return@ioTask
                    }
                    toast("${mcFile.name} 更新完成")
                }
            }

            val uiHash = server.makeRequest<String>("update/ui/hash").data?:throw RequestError("获取ui核心版本信息失败")
            val uiNeedsUpdate = !uiJarFile.exists() || uiJarFile.sha1 != uiHash

            if (uiNeedsUpdate) {
                logInfo("准备下载 ${uiJarFile.name}...")
                val uiUpdated = downloadAndReplaceCore(
                    targetFile = uiJarFile,
                    downloadUrl = "${server.hqUrl}/update/ui",
                    expectedSha = uiHash,
                    label = uiJarFile.name
                )
                if (uiUpdated) {
                    startRestartCountdown()
                } else {
                    showRetryOption()
                }
                return@ioTask
            }

            lgr.info { "是最新版核心" }
            toast("当前已是最新版核心")
            continueToLogin()
        }.onFailure {
            lgr.error(it) { "核心更新流程失败" }
            logInfo("更新流程遇到错误：${it.message ?: it::class.simpleName}")
            showRetryOption()
        }
    }

    private suspend fun downloadAndReplaceCore(
        targetFile: File,
        downloadUrl: String,
        expectedSha: String,
        label: String
    ): Boolean {
        val parentDir = targetFile.absoluteFile.parentFile ?: File(".")
        if (!parentDir.exists()) parentDir.mkdirs()
        val tempFile = File(parentDir, "${targetFile.name}.downloading.${System.currentTimeMillis()}")

        var progressView: TextView? = null
        uiThread {
            progressView = contentView.textView("正在下载 $label ...") {
                layoutParams = linearLayoutParam(PARENT, SELF).apply {
                    setMargins(0, 0, 0, dp(8f))
                }
            }
        }

        tempFile.toPath().downloadFileFrom(downloadUrl) { dl ->
            val totalBytes = dl.totalBytes
            val downloadedBytes = dl.bytesDownloaded.takeIf { it >= 0 } ?: 0L
            val percentValue = when {
                totalBytes > 0 -> downloadedBytes * 100.0 / totalBytes
                dl.percent >= 0 -> dl.percent.toDouble()
                else -> -1.0
            }

            val percentText = percentValue.takeIf { it >= 0 }
                ?.let { String.format("%.1f%%", it) } ?: "--"
            val downloadedText = downloadedBytes.takeIf { it > 0 }?.humanFileSize ?: "0B"
            val totalText = totalBytes.takeIf { it > 0 }?.humanFileSize ?: "--"
            val speedText = dl.speedBytesPerSecond.takeIf { it > 0 }
                ?.let { "${it / 1000}KB/s" } ?: "--"

            uiThread {
                progressView?.text = "下载中：$percentText $downloadedText/$totalText $speedText"
            }
        }.getOrElse {
            tempFile.delete()
            logInfo("下载失败，请检查网络后重试。${it.message}")
            return false
        }

        val downloadedSha = tempFile.sha1
        if (!downloadedSha.equals(expectedSha, true)) {
            tempFile.delete()
            logInfo("文件损坏了，请重下")
            return false
        }

        fun deleteWithRetry(file: File, retries: Int = 5, delayMs: Long = 200): Boolean {
            repeat(retries) { attempt ->
                if (!file.exists() || file.delete()) return true
                Thread.sleep(delayMs)
            }
            return !file.exists()
        }

        fun tryOverwriteEvenIfLocked(src: File, dst: File): Boolean = runCatching {
            FileInputStream(src).channel.use { inCh ->
                FileOutputStream(dst, false).channel.use { outCh ->
                    outCh.truncate(0)
                    var pos = 0L
                    val size = inCh.size()
                    while (pos < size) {
                        val transferred = inCh.transferTo(pos, 1024 * 1024, outCh)
                        if (transferred <= 0) break
                        pos += transferred
                    }
                }
            }
            true
        }.getOrElse { false }

        val backupFile = if (targetFile.exists()) File(
            parentDir,
            "${targetFile.name}.backup.${System.currentTimeMillis()}"
        ) else null

        val replaced = runCatching {
            backupFile?.let { targetFile.copyTo(it, overwrite = true) }

            if (targetFile.exists() && !deleteWithRetry(targetFile)) {
                targetFile.deleteOnExit()
                // attempt overwrite without delete
                val overwritten = tryOverwriteEvenIfLocked(tempFile, targetFile)
                if (!overwritten) {
                    throw IllegalStateException("无法删除旧文件: ${targetFile.absolutePath}")
                }
                tempFile.delete()
                return@runCatching
            }

            Files.move(
                tempFile.toPath(),
                targetFile.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }.recoverCatching {
            // Move may fail on some filesystems; fallback to copy + delete or overwrite
            if (targetFile.exists() && !deleteWithRetry(targetFile)) {
                targetFile.deleteOnExit()
                if (!tryOverwriteEvenIfLocked(tempFile, targetFile)) {
                    throw IllegalStateException("无法删除旧文件: ${targetFile.absolutePath}")
                }
                tempFile.delete()
            } else {
                Files.copy(
                    tempFile.toPath(),
                    targetFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
                tempFile.delete()
            }
        }.onFailure { err ->
            logInfo("替换核心文件失败：${err.message}")
            tempFile.delete()
            backupFile?.let { backup ->
                runCatching {
                    if (!targetFile.exists() && backup.exists()) {
                        Files.move(
                            backup.toPath(),
                            targetFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                        )
                    }
                }
            }
        }.isSuccess

        if (replaced) {
            backupFile?.delete()
            logInfo("核心文件已更新至最新版本。")
        }

        return replaced
    }

    private fun showRetryOption(): Unit = uiThread {
        if (retryButtonAdded) return@uiThread
        retryButtonAdded = true
        contentView.button( "重试更新") { button: RButton ->
            button.isEnabled = false
            contentView.removeAllViews()
            retryButtonAdded = false
            startUpdateFlow()
        }
    }

    private fun continueToLogin() = uiThread {

        LoginFragment().go(false)
    }

    private fun startRestartCountdown() {
        exitProcess(0)
        /*uiThread {
            val countdownView = contentView.textView("更新完成，客户端将在 5 秒后重启...") {
                layoutParams = linearLayoutParam(PARENT, SELF).apply {
                    setMargins(0, dp(16f), 0, 0)
                }
                setTextColor(MaterialColor.GREEN_500.colorValue)
            }

            ioTask {
                for (i in 5 downTo 1) {
                    uiThread { countdownView.text = "更新完成，客户端将在 $i 秒后重启..." }
                    Thread.sleep(1000)
                }
                uiThread {
                    countdownView.text = "正在重启客户端..."
                    countdownView.setTextColor(MaterialColor.BLUE_500.colorValue)
                }

            }
        }*/
    }



}