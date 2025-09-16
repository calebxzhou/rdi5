package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.lgr
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.body
import calebxzhou.rdi.net.downloadFileWithProgress
import calebxzhou.rdi.net.formatBytes
import calebxzhou.rdi.net.formatSpeed
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.util.*
import com.electronwill.nightconfig.core.Config
import com.electronwill.nightconfig.toml.TomlFormat
import icyllis.modernui.view.View
import icyllis.modernui.widget.LinearLayout
import icyllis.modernui.widget.ScrollView
import java.io.File
import java.security.MessageDigest
import java.util.jar.JarFile
import kotlin.system.exitProcess

class UpdateFragment(val server: RServer) : RFragment("正在检查更新") {
    init {

    }
    val modDir = System.getProperty("rdi.updateModDir")?.let { File(it) } ?: File("mods")
    lateinit var view: ScrollView
    lateinit var scrollContent: LinearLayout
    override var closable = false
    // Helper function to scroll to bottom
    private fun scrollToBottom() {
        view.post {
            // Try multiple approaches to ensure scrolling works
            try {
                // Approach 1: fullScroll if available
                view.fullScroll(View.FOCUS_DOWN)
            } catch (e: Exception) {
                try {
                    // Approach 2: smoothScrollTo to bottom
                    view.smoothScrollTo(0,)
                } catch (e2: Exception) {
                    try {
                        // Approach 3: scrollTo to the maximum scroll position
                        val maxScroll = scrollContent.height - view.height
                        if (maxScroll > 0) {
                            view.scrollTo(0, maxScroll)
                        }
                    } catch (e3: Exception) {
                        // If all methods fail, just ignore
                        lgr.warn("Failed to scroll to bottom: ${e3.message}")
                    }
                }
            }
        }
    }
    
    override fun initContent() {
        contentLayout.apply {
            iconButton("next", "跳过更新，一会再说",{
                layoutParams = linearLayoutParam(SELF, SELF)
            }) {
                goto(SelectAccountFragment())
            }
            view = scrollView {
                layoutParams = frameLayoutParam(PARENT, PARENT)
                // Create a LinearLayout as the scrollable content container
                scrollContent = linearLayout {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = frameLayoutParam(PARENT, SELF)
                    paddingDp(16)
                }
            }
        }
        ioTask {
            val modsToUpdate = server.checkUpdate(modDir)
            if (modsToUpdate.isEmpty()) {
                lgr.info("没有需要更新的mod")
                close()
                goto(SelectAccountFragment())
                return@ioTask
            }
            uiThread {
                scrollContent.apply {
                    textView("这些mod需要更新：\n${modsToUpdate.map { it.key }.joinToString(",")}") {
                        layoutParams = linearLayoutParam(PARENT, SELF).apply {
                            setMargins(0, 0, 0, dp(16f))
                        }
                    }
                    iconButton("success", "立刻更新", init = {
                        layoutParams = linearLayoutParam(SELF, SELF).apply {
                            setMargins(0, 0, 0, dp(8f))
                        }
                    }, onClick = { button ->
                        // Immediately disable button and update UI
                        button.isEnabled = false
                        button.text = "正在准备下载..."
                        
                        // Immediately show preparation message
                        scrollContent.removeAllViews()
                        scrollContent.textView("正在准备下载更新，请稍候...") {
                            layoutParams = linearLayoutParam(PARENT, SELF).apply {
                                setMargins(0, 0, 0, dp(16f))
                            }
                        }
                        scrollToBottom()
                        
                        // Start download process asynchronously
                        ioTask {
                            // Get server SHA-1 checksums for verification
                            val modlist = server.prepareRequest(false, "update/mod-list").body
                            val serverIdSha1: Map<String, String> = serdesJson.decodeFromString(modlist)
                            server.downloadMods(modsToUpdate, serverIdSha1, this@UpdateFragment)
                        }
                    })

                }
            }
        }
    }

    private fun calculateSha1(file: File): String {
        val digest = MessageDigest.getInstance("SHA-1")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }


    //获取需要更新的mod id to file
    fun gatherModIdFile(modsDir: File): Map<String, File> {
        val idFile = hashMapOf<String, File>()
        modsDir.listFiles { it.extension == "jar" }?.forEach { jarFile ->
            try {
                JarFile(jarFile).use { jar ->
                    jar.getJarEntry("META-INF/neoforge.mods.toml")?.let { modsTomlEntry ->
                        //解析toml
                        jar.getInputStream(modsTomlEntry).bufferedReader().use { reader ->
                            val modId = TomlFormat.instance()
                                .createParser()
                                .parse(reader.readText())
                                .get<List<Config>>("mods")
                                .first()
                                .get<String>("modId")
                            idFile += modId to jarFile
                        }
                    }
                }
            } catch (e: Exception) {
                // 如果mod文件损坏，记录日志并跳过，这样该文件将被标记为需要更新
                lgr.warn("mod文件损坏，将被标记为需要更新: ${jarFile.name}", e)
                // 对于损坏的文件，我们使用文件名（去掉.jar扩展名）作为临时mod ID
                val tempModId = jarFile.nameWithoutExtension
                idFile += tempModId to jarFile
            }
        }
        lgr.info("mod安装目录：$modsDir")
        return idFile
    }

    //获取mod id to sha1的映射
    fun gatherModIdSha1(modsDir: File): Map<String, String> {
        val idSha1 = hashMapOf<String, String>()
        modsDir.listFiles { it.extension == "jar" }?.forEach { jarFile ->
            try {
                JarFile(jarFile).use { jar ->
                    jar.getJarEntry("META-INF/neoforge.mods.toml")?.let { modsTomlEntry ->
                        //解析toml
                        jar.getInputStream(modsTomlEntry).bufferedReader().use { reader ->
                            val modId = TomlFormat.instance()
                                .createParser()
                                .parse(reader.readText())
                                .get<List<Config>>("mods")
                                .first()
                                .get<String>("modId")
                            idSha1 += modId to calculateSha1(jarFile)
                        }
                    }
                }
            } catch (e: Exception) {
                // 如果mod文件损坏，使用特殊的SHA-1值表示损坏状态
                lgr.warn("mod文件损坏，无法计算SHA-1: ${jarFile.name}", e)
                val tempModId = jarFile.nameWithoutExtension
                // 使用特殊值表示文件损坏，确保与服务端SHA-1不匹配
                idSha1 += tempModId to "corrupted_file_${System.currentTimeMillis()}"
            }
        }
        return idSha1
    }

    //返回需要更新的mod列表
    suspend fun RServer.checkUpdate(modsDir: File): Map<String, File> {
        val clientIdFile = gatherModIdFile(modsDir)
        val clientIdSha1 = gatherModIdSha1(modsDir)
        val modlist = prepareRequest(false, "update/mod-list").body
        val modsUpdate = hashMapOf<String, File>()
        //服务端
        val serverIdSha1: Map<String, String> = serdesJson.decodeFromString(modlist)
        serverIdSha1.forEach { id, serverSha1 ->
            clientIdFile[id]?.let { file ->
                val clientSha1 = clientIdSha1[id]
                if (clientSha1 != serverSha1) {
                    modsUpdate += id to file
                }
            } ?: let { modsUpdate += id to File(modsDir, "$id.jar") }
        }
        /*notifyOs(
            "以下mod需要更新:${modsStrDisp}.正在更新。"
        )
       */
        return modsUpdate
    }

    suspend fun RServer.downloadMods(mods: Map<String, File>, serverIdSha1: Map<String, String>, fragment: UpdateFragment) {
        // UI has already been updated in the button click handler
        
        var updateFailed = false
        var failedMods = mutableListOf<String>()
        
        // Clean up any leftover temporary files from previous failed attempts
        try {
            mods.values.forEach { file ->
                file.parentFile?.listFiles { _, name -> 
                    name.startsWith("${file.name}.tmp.") || name.startsWith("${file.name}.backup.")
                }?.forEach { 
                    lgr.debug("Cleaning up leftover file: ${it.name}")
                    it.delete() 
                }
            }
        } catch (e: Exception) {
            lgr.warn("Failed to clean up leftover temporary files", e)
        }
        
        // Download each mod with progress updates and SHA-1 verification
        val totalMods = mods.size
        var currentMod = 0
        
        mods.forEach { (id, file) ->
            currentMod++
            val progressText = "($currentMod/$totalMods)"
            
            // Create a progress TextView that will be updated during download
            var progressTextView: icyllis.modernui.widget.TextView? = null
            uiThread {
                progressTextView = fragment.scrollContent.textView("$progressText 开始下载mod： ${id}") {
                    layoutParams = linearLayoutParam(PARENT, SELF).apply {
                        setMargins(0, 0, 0, dp(8f))
                    }
                }
                fragment.scrollToBottom()
            }
            
            try {
                // Create temporary file for safe download
                val tempFile = File(file.parent, "${file.name}.tmp.${System.currentTimeMillis()}")
                
                val downloadSuccess = downloadFileWithProgress(
                    hqUrl + "update/mod-file?modid=${id}",
                    tempFile.toPath()
                ) { bytesDownloaded, totalBytes, speed ->
                    // Update progress in UI thread
                    uiThread {
                        progressTextView?.let { textView ->
                            val progressInfo = if (totalBytes > 0) {
                                val percentage = (bytesDownloaded * 100 / totalBytes).toInt()
                                val downloadedStr = formatBytes(bytesDownloaded)
                                val totalStr = formatBytes(totalBytes)
                                val speedStr = formatSpeed(speed)
                                "$progressText 下载中 ${id} - $percentage% ($downloadedStr/$totalStr) $speedStr"
                            } else {
                                val downloadedStr = formatBytes(bytesDownloaded)
                                val speedStr = formatSpeed(speed)
                                "$progressText 下载中 ${id} - $downloadedStr $speedStr"
                            }
                            textView.text = progressInfo
                        }
                        fragment.scrollToBottom()
                    }
                }
                
                if (downloadSuccess) {
                    uiThread {
                        progressTextView?.let { textView ->
                            textView.text = "$progressText ✓ ${id}下载完成，正在验证文件完整性..."
                            textView.setTextColor(0xFF2196F3.toInt()) // Blue color
                        }
                        fragment.scrollToBottom()
                    }
                    
                    // Verify SHA-1 checksum of temporary file
                    val downloadedSha1 = fragment.calculateSha1(tempFile)
                    val expectedSha1 = serverIdSha1[id]
                    
                    if (expectedSha1 != null && downloadedSha1 == expectedSha1) {
                        // Validation successful - atomically replace original file
                        try {
                            // Backup original file if it exists
                            var backupFile: File? = null
                            if (file.exists()) {
                                backupFile = File(file.parent, "${file.name}.backup.${System.currentTimeMillis()}")
                                file.renameTo(backupFile)
                            }
                            
                            // Move temp file to final location
                            if (tempFile.renameTo(file)) {
                                // Success - delete backup if it exists
                                backupFile?.delete()
                                
                                uiThread {
                                    progressTextView?.let { textView ->
                                        textView.text = "$progressText ✓ ${id}验证成功！"
                                        textView.setTextColor(0xFF4CAF50.toInt()) // Green color
                                    }
                                    fragment.scrollToBottom()
                                }
                            } else {
                                // File move failed - restore backup
                                backupFile?.renameTo(file)
                                throw Exception("Failed to move temporary file to final location")
                            }
                        } catch (e: Exception) {
                            updateFailed = true
                            failedMods.add(id)
                            lgr.error("Failed to replace file for $id", e)
                            uiThread {
                                progressTextView?.let { textView ->
                                    textView.text = "$progressText ✗ ${id}文件替换失败！可能是文件被其他程序占用了，请退出后重试"
                                    textView.setTextColor(0xFFE53E3E.toInt()) // Red color
                                }
                                fragment.scrollToBottom()
                            }
                        }
                    } else {
                        // Validation failed - clean up temp file
                        tempFile.delete()
                        updateFailed = true
                        failedMods.add(id)
                        uiThread {
                            progressTextView?.let { textView ->
                                textView.text = "$progressText ✗ ${id}文件校验失败！"
                                textView.setTextColor(0xFFE53E3E.toInt()) // Red color
                            }
                            fragment.scrollToBottom()
                        }
                        lgr.warn("SHA-1 verification failed for $id. Expected: $expectedSha1, Got: $downloadedSha1")
                    }
                } else {
                    // Download failed - clean up temp file
                    tempFile.delete()
                    updateFailed = true
                    failedMods.add(id)
                    uiThread {
                        progressTextView?.let { textView ->
                            textView.text = "$progressText ✗ ${id}下载失败！"
                            textView.setTextColor(0xFFE53E3E.toInt()) // Red color
                        }
                        fragment.scrollToBottom()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                updateFailed = true
                failedMods.add(id)
                
                // Clean up any temporary files that might exist
                try {
                    val tempFile = File(file.parent, "${file.name}.tmp.${System.currentTimeMillis()}")
                    if (tempFile.exists()) {
                        tempFile.delete()
                    }
                    // Also clean up any temp files with similar patterns (in case of timing issues)
                    file.parentFile?.listFiles { _, name -> 
                        name.startsWith("${file.name}.tmp.") 
                    }?.forEach { it.delete() }
                } catch (cleanupException: Exception) {
                    lgr.warn("Failed to clean up temporary files for $id", cleanupException)
                }
                
                uiThread {
                    progressTextView?.let { textView ->
                        textView.text = "$progressText ✗ ${id}下载失败，详见日志！"
                        textView.setTextColor(0xFFE53E3E.toInt()) // Red color
                    }
                    fragment.scrollToBottom()
                }
            }
        }
        
        if (updateFailed) {
            // Handle update failure case
            uiThread {
                fragment.scrollContent.apply {
                    textView("更新失败！以下mod下载失败：${failedMods.joinToString(", ")}") {
                        layoutParams = linearLayoutParam(PARENT, SELF).apply {
                            setMargins(0, dp(16f), 0, dp(16f))
                        }
                        setTextColor(0xFFE53E3E.toInt()) // Red color
                    }
                    
                    // Add cancel button that navigates to TitleFragment
                    iconButton("error", "取消更新", init = {
                        layoutParams = linearLayoutParam(PARENT, SELF).apply {
                            setMargins(0, 0, 0, dp(8f))
                        }
                    }, onClick = {
                        fragment.close()
                        goto(TitleFragment())
                    })
                }
                fragment.scrollToBottom()
            }
        } else {
            // Handle success case with countdown
            uiThread {
                val countdownText = fragment.scrollContent.textView("更新完成！客户端将在 5 秒后重启...") {
                    layoutParams = linearLayoutParam(PARENT, SELF).apply {
                        setMargins(0, dp(16f), 0, 0)
                    }
                    setTextColor(0xFF4CAF50.toInt()) // Green color
                }
                fragment.scrollToBottom()
                
                // Start countdown
                ioTask {
                    for (i in 5 downTo 1) {
                        uiThread {
                            countdownText.text = "更新完成！客户端将在 $i 秒后重启..."
                        }
                        Thread.sleep(1000)
                    }
                    
                    uiThread {
                        countdownText.text = "正在重启客户端..."
                        countdownText.setTextColor(0xFF2196F3.toInt()) // Blue color
                    }
                    
                    notifyOs("更新完成，客户端即将重启")
                    restart()
                }
            }
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