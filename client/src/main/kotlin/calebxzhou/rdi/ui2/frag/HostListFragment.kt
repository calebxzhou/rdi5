package calebxzhou.rdi.ui2.frag

// Spinner replaced by radio buttons
import calebxzhou.rdi.auth.LocalCredentials
import calebxzhou.rdi.model.Host
import calebxzhou.rdi.model.World
import calebxzhou.rdi.model.account
import calebxzhou.rdi.net.server
import calebxzhou.rdi.service.isOwnerOrAdmin
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
import io.ktor.http.*
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.multiplayer.resolver.ServerAddress
import org.bson.types.ObjectId

class HostListFragment() : RFragment("团队的服务器") {
    companion object {
        var screen: Screen? = null
    }

    override var fragSize = FragmentSize.SMALL

    init {
        bottomOptionsConfig = {

        }
        contentViewInit = {
            load()

            if (isMcStarted)
                screen = this@HostListFragment.mcScreen
        }
    }

    fun load() = ioTask {
    }

    /*fun render(team: Team, hosts: List<Host>) = uiThread {
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
*/
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


}