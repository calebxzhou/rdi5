package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.auth.LocalCredentials
import calebxzhou.rdi.model.Host
import calebxzhou.rdi.model.Team
import calebxzhou.rdi.model.World
import calebxzhou.rdi.model.account
import calebxzhou.rdi.model.pack.ModpackInfo
import calebxzhou.rdi.net.server
import calebxzhou.rdi.service.isOwnerOrAdmin
import calebxzhou.rdi.service.myTeam
import calebxzhou.rdi.service.myTeamHosts
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.component.alertErr
import calebxzhou.rdi.ui2.component.confirm
import calebxzhou.rdi.ui2.misc.contextMenu
import calebxzhou.rdi.util.ioTask
import calebxzhou.rdi.util.isMcStarted
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.renderThread
import icyllis.modernui.widget.LinearLayout
import icyllis.modernui.widget.Spinner
import io.ktor.http.*
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.multiplayer.resolver.ServerAddress
import net.minecraft.commands.arguments.TeamArgument.team

class HostListFragment() : RFragment("选择主机") {
    companion object{
        var screen: Screen? = null
    }
    override var fragSize = FragmentSize.SMALL

    init {
        bottomOptionsConfig = {
            "＋ 创建主机" colored MaterialColor.BLUE_900 with {
                Create(null,::load).go()
            }
            "\uEF09 选择节点" with { Carrier().go() }
        }
        contentViewInit = {
            load()

            if(isMcStarted)
            screen = this@HostListFragment.mcScreen
        }
    }

    fun load() = ioTask{
        account.myTeam()?.let { t->
            account.myTeamHosts()?.let { h ->
                render(t,h)
            }
        }
    }

    fun render(team:Team,hosts: List<Host>) = uiThread {
        contentView.removeAllViews()
        contentView.apply {
            linearLayout {
                padding8dp()
                textView("🖱点击开始游玩")
                if (team.isOwnerOrAdmin(account)) {
                    textView("，右键进行管理")
                }
            }
            hosts.forEach { host ->
                button(
                    "\uF233   ${host.name}", init = {
                        if (team.isOwnerOrAdmin(account)) {
                            contextMenu {
                                "删除" with {
                                    confirm("要删除主机“${host.name}”吗？\n（存档会被保留）") {
                                        server.request<Unit>(
                                            "host/${host._id}",
                                            HttpMethod.Delete,
                                            showLoading = true
                                        ) {
                                            toast("已删除")
                                            reloadFragment()
                                        }
                                    }
                                }
                                "后台" with {
                                    HostConsoleFragment(host).go()
                                }
                                "Mod列表" with {
                                    HostModFragment(host._id).go()
                                }
                                "切换存档" with {
                                    alertErr("没开发完呢")
                                }
                                "更新整合包" with {
                                    confirm("将更新主机“${host.name}”的整合包到最新版本。\n主机会关闭，更新时间大概需要15秒\n（除存档外，所有数据会被删除，包括日志、附加Mod等）\n附加mod需要你在主机mod管理页面手动点击“重新下载”") {
                                        server.requestU(
                                            "host/${host._id}/update",
                                            HttpMethod.Post,
                                            showLoading = true
                                        ) {
                                            toast("已更新到最新版 主机重启中")
                                        }
                                    }
                                }
                            }
                        }
                    }, onClick =
                        { play(host) }
                )
            }
            if (hosts.isEmpty()) {
                textView("没有主机，请点击创建按钮")
            }
        }
    }

    private fun play(host: Host) {
        //电信以外全bgp
        val bgp = LocalCredentials.read().carrier != 0
        server.request<String>("host/${host._id}/status") {

            if (it.data == "STARTED") {
                alertErr("主机正在载入中\n请稍等1~5分钟")
                return@request
            } else if (it.data == "STOPPED") {
                alertErr("需要队长/管理者在后台启动主机")
                return@request
            }
            Host.now = host
            ioTask {
                renderThread {
                    ConnectScreen.startConnecting(
                        this@HostListFragment.mcScreen,
                        mc,
                        ServerAddress(if (bgp) server.bgpIp else server.ip, server.gamePort),
                        server.mcData(bgp),
                        false,
                        null
                    )
                }
            }

        }
    }

    class Create(val modpack: ModpackInfo?=null,val onOk: () -> Unit={}) : RFragment("创建主机") {
        private lateinit var worldSpinner: Spinner
        override var fragSize = FragmentSize.SMALL
        private var worlds: List<World> = emptyList()

        init {
            contentViewInit = {
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
                        contentView.apply {
                            minimumWidth = 500
                            center()
                            linearLayout {
                                modpack?.let { textView("已选择整合包：${it.name}") }
                                    ?:let {
                                        textView("未选择整合包 默认原版空岛")
                                        button("选包"){ ModpackListFragment().go(false)}
                                    }

                            }
                            linearLayout {
                                textView("选择存档")
                                worldSpinner = spinner(displayEntries)
                            }
                        }
                        contentView.bottomOptions {
                            "创建" colored MaterialColor.GREEN_900 with {
                                val selectedWorld = worlds.getOrNull(worldSpinner.selectedItemPosition)
                                val params =
                                    selectedWorld?.let { mapOf("worldId" to it._id) } ?: emptyMap<String, Any>()
                                server.requestU("host/", HttpMethod.Post, params) {
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

    class Carrier : RFragment("选择运营商节点") {
        override var fragSize  = FragmentSize.SMALL
        private val creds = LocalCredentials.read()
        private val carriers = arrayListOf("电信", "移动", "联通", "教育网", "广电")
        override var contentViewInit: LinearLayout.() -> Unit = {
            radioGroup {
                center()
                carriers.forEachIndexed { i, c ->
                    radioButton(c) {
                        id = i
                        isSelected = creds.carrier == i
                    }
                }
                check(creds.carrier)
                setOnCheckedChangeListener { g, id ->
                    creds.carrier = id
                    creds.save()
                }
            }
        }
    }
}