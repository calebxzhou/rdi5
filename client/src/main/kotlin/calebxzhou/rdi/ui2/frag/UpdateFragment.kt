package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.lgr
import calebxzhou.rdi.model.RServer
import calebxzhou.rdi.net.body
import calebxzhou.rdi.net.downloadFile
import calebxzhou.rdi.net.success
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.util.*
import com.electronwill.nightconfig.core.Config
import com.electronwill.nightconfig.toml.TomlFormat
import icyllis.modernui.view.View
import icyllis.modernui.widget.LinearLayout
import icyllis.modernui.widget.ScrollView
import java.io.File
import java.util.jar.JarFile
import kotlin.system.exitProcess

class UpdateFragment(val server: RServer) : RFragment("检查更新") {
    init {

    }

    lateinit var view: ScrollView
    lateinit var scrollContent: LinearLayout
    
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
            val modsToUpdate = server.checkUpdate(File("mods"))
            if (modsToUpdate.isEmpty()) {
                lgr.info("没有需要更新的mod")
                close()
                mc go SelectAccountFragment(server)
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
                        layoutParams = linearLayoutParam(PARENT, SELF).apply {
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
                            server.downloadMods(modsToUpdate, this@UpdateFragment)
                        }
                    })
                    iconButton("error", "先不更新，一会再说") {
                        layoutParams = linearLayoutParam(PARENT, SELF)
                        close()
                        mc go SelectAccountFragment(server)
                    }
                }
            }
        }
    }

    //获取需要更新的mod id to file
    fun gatherModIdFile(modsDir: File): Map<String, File> {
        val idFile = hashMapOf<String, File>()
        modsDir.listFiles { it.extension == "jar" }.forEach { jarFile ->
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
        }
        lgr.info("mod安装目录：$modsDir")
        return idFile
    }

    //返回需要更新的mod列表
    suspend fun RServer.checkUpdate(modsDir: File): Map<String, File> {
        val clientIdFile = gatherModIdFile(modsDir)
        val modlist = prepareRequest(false, "update/mod-list").body
        val modsUpdate = hashMapOf<String, File>()
        //服务端
        val serverIdSize: Map<String, Long> = serdesJson.decodeFromString(modlist)
        serverIdSize.forEach { id, size ->
            clientIdFile[id]?.let { file ->
                if (file.length() != size) {
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

    suspend fun RServer.downloadMods(mods: Map<String, File>, fragment: UpdateFragment) {
        // UI has already been updated in the button click handler
        
        // Download each mod with progress updates
        mods.forEach { (id, file) ->
            uiThread {
                fragment.scrollContent.textView("开始下载mod： ${id}") {
                    layoutParams = linearLayoutParam(PARENT, SELF).apply {
                        setMargins(0, 0, 0, dp(8f))
                    }
                }
                fragment.scrollToBottom()
            }
            
            try {
                val response = downloadFile(
                    hqUrl + "update/mod-file?modid=${id}",
                    file.toPath()
                )
                if (response.success) {
                    uiThread {
                        fragment.scrollContent.textView("✓ ${id}下载成功！") {
                            layoutParams = linearLayoutParam(PARENT, SELF).apply {
                                setMargins(0, 0, 0, dp(8f))
                            }
                            setTextColor(0xFF4CAF50.toInt()) // Green color
                        }
                        fragment.scrollToBottom()
                    }
                } else {
                    uiThread {
                        fragment.scrollContent.textView("✗ ${id}下载失败！") {
                            layoutParams = linearLayoutParam(PARENT, SELF).apply {
                                setMargins(0, 0, 0, dp(8f))
                            }
                            setTextColor(0xFFE53E3E.toInt()) // Red color
                        }
                        fragment.scrollToBottom()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                uiThread {
                    fragment.scrollContent.textView("✗ ${id}下载失败，详见日志！") {
                        layoutParams = linearLayoutParam(PARENT, SELF).apply {
                            setMargins(0, 0, 0, dp(8f))
                        }
                        setTextColor(0xFFE53E3E.toInt()) // Red color
                    }
                    fragment.scrollToBottom()
                }
            }
        }
        
        uiThread {
            fragment.scrollContent.textView("更新完成，客户端即将重启") {
                layoutParams = linearLayoutParam(PARENT, SELF).apply {
                    setMargins(0, dp(16f), 0, 0)
                }
                setTextColor(0xFF2196F3.toInt()) // Blue color
            }
            fragment.scrollToBottom()
        }
        
        notifyOs("更新完成，客户端即将重启")
        restart()
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