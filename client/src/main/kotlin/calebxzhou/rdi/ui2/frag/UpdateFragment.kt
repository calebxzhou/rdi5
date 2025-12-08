package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.lgr
import calebxzhou.rdi.net.downloadFileWithProgress
import calebxzhou.rdi.net.humanSize
import calebxzhou.rdi.net.server
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.component.RButton
import calebxzhou.rdi.ui2.component.confirm
import calebxzhou.rdi.util.ioTask
import calebxzhou.rdi.util.notifyOs
import calebxzhou.rdi.util.sha1
import icyllis.modernui.widget.TextView
import java.io.File
import kotlin.system.exitProcess

class UpdateFragment() : RFragment("正在检查更新") {

    private val coreJarFile: File = resolveCoreJarFile()
    private var retryButtonAdded = false
    override var fragSize: FragmentSize
        get() = FragmentSize.SMALL
        set(value) {}

    override var closable = false

    init {
        titleViewInit = {
            quickOptions {
                "> 跳过更新" with { LoginFragment().go() }
            }
        }
        contentViewInit = {
            startUpdateFlow()
        }
    }

    private fun resolveCoreJarFile(): File {
        val explicit = System.getProperty("rdi.coreJar")?.let { File(it) }
        val candidates = buildList {
            explicit?.let { add(it) }
            add(File("mods/rdi-5-client.jar"))
        }.map { it.normalize().absoluteFile }
        return candidates.firstOrNull { it.exists() } ?: candidates.first()
    }

    private fun startUpdateFlow() {
        runCatching {
            server.request<String>("update/hash") {
                val localHash = coreJarFile.sha1
                val serverHash = it.data!!
                if (serverHash == localHash) {
                    lgr.info("是最新版rdi核心")
                    toast("当前已是最新版RDI核心")
                    continueToLogin()
                    return@request
                } else {
                    confirm("RDI核心有新版了，要自动更新吗？") {
                        logInfo("准备下载 ${coreJarFile.name}...")
                        ioTask {
                            val updated = downloadAndReplaceCore(serverHash)
                            if (updated) {
                                notifyOs("RDI 核心更新完成，客户端即将重启")
                                startRestartCountdown()
                            } else {
                                showRetryOption()
                            }
                        }
                    }
                }

            }


        }.onFailure {
            lgr.error("核心更新流程失败", it)
            logError("更新流程遇到错误：${it.message ?: it::class.simpleName}")
            showRetryOption()
        }
    }

    private suspend fun downloadAndReplaceCore(expectedSha: String): Boolean {
        val parentDir = coreJarFile.absoluteFile.parentFile ?: File(".")
        if (!parentDir.exists()) parentDir.mkdirs()
        val tempFile = File(parentDir, "${coreJarFile.name}.downloading.${System.currentTimeMillis()}")

        var progressView: TextView? = null
        uiThread {
            progressView = contentView.textView("正在下载 ${coreJarFile.name} ...") {
                layoutParams = linearLayoutParam(PARENT, SELF).apply {
                    setMargins(0, 0, 0, dp(8f))
                }
            }
        }

        val downloadUrl = "${server.hqUrl}/update/core"
        val downloadSuccess = downloadFileWithProgress(downloadUrl, tempFile.toPath()) { dl ->

            uiThread {
                progressView?.text =
                    "下载中：${dl.percent} ${dl.bytesDownloaded.humanSize}/${dl.totalBytes.humanSize} ${dl.speedBytesPerSecond / 1000}KB/s"

            }
        }

        if (!downloadSuccess) {
            tempFile.delete()
            logError("下载失败，请检查网络后重试。")
            return false
        }

        val downloadedSha = tempFile.sha1
        if (!downloadedSha.equals(expectedSha, true)) {
            tempFile.delete()
            logError("文件损坏了，请重下")
            return false
        }

        val backupFile = if (coreJarFile.exists()) File(
            parentDir,
            "${coreJarFile.name}.backup.${System.currentTimeMillis()}"
        ) else null

        return try {
            backupFile?.let { coreJarFile.copyTo(it, overwrite = true) }
            tempFile.copyTo(coreJarFile, overwrite = true)
            tempFile.delete()
            backupFile?.delete()
            logSuccess("核心文件已更新至最新版本。")
            true
        } catch (t: Throwable) {
            logError("替换核心文件失败：${t.message}")
            tempFile.delete()
            backupFile?.let { backup ->
                runCatching {
                    if (!coreJarFile.exists() && backup.exists()) {
                        backup.renameTo(coreJarFile)
                    }
                }
            }
            false
        }
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
        uiThread {
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
                restart()
            }
        }
    }

    private fun logInfo(message: String) = logMessage(message, MaterialColor.GRAY_200.colorValue)
    private fun logSuccess(message: String) = logMessage(message, MaterialColor.GREEN_600.colorValue)
    private fun logError(message: String) = logMessage(message, MaterialColor.RED_500.colorValue)

    private fun logMessage(message: String, color: Int?) = uiThread {
        contentView.textView(message) {
            layoutParams = linearLayoutParam(PARENT, SELF).apply {
                setMargins(0, 0, 0, dp(6f))
            }
            color?.let { setTextColor(it) }
        }
    }


    fun restart() {
        try {
            ProcessBuilder("../../../PCL/LatestLaunch.bat").start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        exitProcess(0)
    }
}