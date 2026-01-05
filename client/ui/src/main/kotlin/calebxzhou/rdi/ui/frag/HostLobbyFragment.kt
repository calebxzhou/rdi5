package calebxzhou.rdi.ui.frag

import calebxzhou.rdi.Const
import calebxzhou.rdi.common.model.Host
import calebxzhou.rdi.net.server
import calebxzhou.rdi.service.GameService
import calebxzhou.rdi.service.ModpackService.startPlay
import calebxzhou.rdi.ui.*
import calebxzhou.rdi.ui.component.HostGrid
import calebxzhou.rdi.ui.component.alertErr
import icyllis.modernui.view.Gravity
import org.bson.types.ObjectId

class HostLobbyFragment : RFragment("大厅") {
    companion object;

    init {
        contentViewInit = {
            loadHosts(true)
        }
        titleViewInit = {
            textView("选择你要游玩的服务器。你可以选择官服，也可以选择其他玩家的自建服")
            quickOptions {
                "\uDB81\uDC8B 我受邀的" make checkbox checked true with {
                    loadHosts(it)

                }
                "MC状态" colored MaterialColor.TEAL_900 with{
                    if(!GameService.started){
                        alertErr("请选择服务器，点击开始键启动MC后再查看状态")
                        return@with
                    }

                }
                "\uEB4B 存档" colored MaterialColor.BLUE_900 with {
                    WorldListFragment().go()
                }
                "\uEF09 节点" with { CarrierFragment().go() }
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
            server.request<Host>("host/${it._id}") {
                it.data?.startPlay()
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