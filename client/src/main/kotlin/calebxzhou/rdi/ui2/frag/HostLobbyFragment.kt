package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.Const
import calebxzhou.rdi.auth.LocalCredentials
import calebxzhou.rdi.model.Host
import calebxzhou.rdi.model.HostStatus
import calebxzhou.rdi.net.server
import calebxzhou.rdi.service.HostClientService
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.component.HostGrid
import calebxzhou.rdi.ui2.component.alertErr
import calebxzhou.rdi.ui2.component.alertOk
import calebxzhou.rdi.util.ioTask
import calebxzhou.rdi.util.isMcStarted
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.renderThread
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.multiplayer.resolver.ServerAddress
import icyllis.modernui.view.Gravity
import org.bson.types.ObjectId

class HostLobbyFragment : RFragment("服务器大厅") {
    companion object {
        var screen: Screen? = null
    }

    init {
        contentViewInit = {
            if (isMcStarted)
                screen = this@HostLobbyFragment.mcScreen
            loadHosts(false)
        }
        titleViewInit = {
            quickOptions {
                "\uDB81\uDC8B 我的服" make checkbox with {
                    loadHosts(it)
                }
                "\uEB4B 我的存档" colored MaterialColor.BLUE_900 with {
                    WorldListFragment().go()
                }
                "\uEF09 切换节点" with { CarrierFragment().go() }
                "\uDB81\uDC90 选包开服" colored MaterialColor.GREEN_900 with {
                    ModpackListFragment().go()
                }
            }
        }
    }

    private fun loadHosts(my: Boolean) {
        if (Const.USE_MOCK_DATA) {
            showHosts(generateMockHosts())
            return
        }

        server.request<List<Host.Vo>>("host/${if (my) "my" else "lobby/0"}") { response ->
            response.data?.let { showHosts(it) }
        }
    }

    private fun showHosts(hosts: List<Host.Vo>) = uiThread {
        contentView.removeAllViews()
        if (hosts.isEmpty()) {
            contentView.textView("暂无可展示的主机") {
                gravity = Gravity.CENTER
                setTextColor(MaterialColor.GRAY_400.colorValue)
                textSize = 16f
                paddingDp(0, 32, 0, 32)
            }
            return@uiThread
        }
        contentView += HostGrid(contentView.context, hosts, { HostInfoFragment(it._id).go() }, {
            if (isMcStarted) {
                HostClientService.play(it._id,it.port,this)
            }
        })
    }


    private fun generateMockHosts(): List<Host.Vo> = List(50) { index ->
        val base = Host.Vo.TEST
        base.copy(
            _id = ObjectId(),
            name = "${base.name} #${index + 1}",
            ownerName = "${base.ownerName} #${index + 1}",
            port = base.port + index
        )
    }
}