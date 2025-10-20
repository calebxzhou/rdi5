package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.Host
import calebxzhou.rdi.model.World
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.server
import calebxzhou.rdi.ui2.FragmentSize
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.button
import calebxzhou.rdi.ui2.center
import calebxzhou.rdi.ui2.go
import calebxzhou.rdi.ui2.iconButton
import calebxzhou.rdi.ui2.linearLayout
import calebxzhou.rdi.ui2.spinner
import calebxzhou.rdi.ui2.textView
import calebxzhou.rdi.ui2.toast
import calebxzhou.rdi.ui2.uiThread
import calebxzhou.rdi.util.ioTask
import icyllis.modernui.widget.Spinner
import io.ktor.http.HttpMethod

class HostListFragment : RFragment("选择主机") {
    override var fragSize = FragmentSize.SMALL

    init {
        bottomOptionsConfig = {
            "＋ 创建主机" colored MaterialColor.BLUE_900 with {
                Create(::load).go()
            }
        }
        contentLayoutInit= {
            load()
        }
    }

    fun load()  {
        server.request<List<Host>>("host/", showLoading = true) {
            render(it.data!!)
        }
    }

    fun render(hosts: List<Host>) = uiThread{
        contentLayout.removeAllViews()
        contentLayout.apply {
            hosts.forEach {
                button("\uF233 ${it.name}")
            }
            if(hosts.isEmpty()){
                textView("没有主机，请点击创建按钮")
            }
        }
    }
    class Create(val onOk: () -> Unit): RFragment("创建主机") {
        private lateinit var worldSpinner: Spinner
        override var fragSize = FragmentSize.SMALL
        private var worlds: List<World> = emptyList()

        init {
            contentLayoutInit = {
                loadWorlds()
            }
        }

        private fun loadWorlds() {
            server.request<List<World>>(
                path = "world/",
                showLoading = true,
                onOk = { response ->
                    worlds = response.data!!
                    uiThread {
                        val displayEntries = if (worlds.isEmpty()) {
                            arrayListOf()
                        } else {
                            worlds.map { it.name }.toMutableList()
                        }
                        displayEntries += "创建新存档"
                        contentLayout.apply {
                            minimumWidth = 500
                            center()
                            linearLayout {
                                textView("选择整合包")
                                spinner(listOf("默认"))
                            }
                            linearLayout {
                                textView("选择存档")
                                worldSpinner = spinner(displayEntries)
                            }
                        }
                        contentLayout.bottomOptions{
                            "创建" colored MaterialColor.GREEN_900 with {
                                val selectedWorld = worlds.getOrNull(worldSpinner.selectedItemPosition)
                                val params = selectedWorld?.let {  mapOf("worldId" to it) } ?: emptyMap<String, Any>()
                                server.requestU("host/", HttpMethod.Post,params ){
                                    close()
                                    toast("创建成功")
                                    onOk()
                                }
                            }
                        }
                    }
                },
                onErr = {
                    toast("拉取存档失败: ${it.msg}")
                }
            )
        }


    }
}