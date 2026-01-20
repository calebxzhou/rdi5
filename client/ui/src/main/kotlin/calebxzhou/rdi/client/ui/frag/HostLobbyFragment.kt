package calebxzhou.rdi.client.ui.frag

import calebxzhou.rdi.client.Const
import calebxzhou.rdi.common.model.Host
import calebxzhou.rdi.client.net.server
import calebxzhou.rdi.client.service.ModpackService.startPlayLegacy
import calebxzhou.rdi.client.ui.*
import calebxzhou.rdi.client.ui.component.HostGrid
import icyllis.modernui.view.Gravity
import org.bson.types.ObjectId

class HostLobbyFragment : RFragment("大厅") {
    companion object;

    init {
        contentViewInit = {
            loadHosts(true)
        }
        titleViewInit = {
            textView("选择你想玩的地图")
            quickOptions {
                "\uDB81\uDC8B 我受邀的" make checkbox  with {
                    loadHosts(it)

                }
                /*"MC状态" colored MaterialColor.TEAL_900 with{
                    if(!GameService.started){
                        alertErr("请选择服务器，点击开始键启动MC后再查看状态")
                        return@with
                    }

                }*/
                "\uEB4B 存档数据" colored MaterialColor.BLUE_900 with {
                    WorldListFragment().go()
                }
                "\uEF09 节点" with { CarrierFragment().go() }
                "\uDB81\uDC90 整合包列表·创建新地图" colored MaterialColor.GREEN_900 with {
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

        server._request<List<Host.BriefVo>>("host/${if (my) "my" else "lobby/0"}") { response ->
            response.data?.let { showHosts(it) }
        }
    }

    private fun showHosts(hosts: List<Host.BriefVo>) = uiThread {
        contentView.removeAllViews()
        if (hosts.isEmpty()) {
            contentView.textView("暂无可展示的") {
                gravity = Gravity.CENTER
                setTextColor(MaterialColor.GRAY_400.colorValue)
                textSize = 16f
                paddingDp(0, 32, 0, 32)
            }
            return@uiThread
        }
        contentView += HostGrid(contentView.context, hosts, { HostInfoFragment(it._id).go() }, {
            server._request<Host>("host/${it._id}") {
                it.data?.startPlayLegacy()
            }
        })
    }


    private fun generateMockHosts(): List<Host.BriefVo> = List(50) { index ->
        val base = Host.BriefVo.TEST
        base.copy(
            _id = ObjectId(),
            name = "${base.name} #${index + 1}",
            port = base.port + index
        )
    }
}