package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.Const
import calebxzhou.rdi.model.Host
import calebxzhou.rdi.net.server
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.component.HostGrid
import org.bson.types.ObjectId

class HostLobbyFragment : RFragment("服务器大厅") {

    private lateinit var hostGrid: HostGrid

    init {
        contentViewInit = {
            hostGrid = HostGrid(context)
            addView(hostGrid, linearLayoutParam(PARENT, PARENT))
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

        server.request<List<Host.Vo>>("host/${if(my) "my" else "lobby/0"}") { response ->
            response.data?.let { showHosts(it) }
        }
    }

    private fun showHosts(hosts: List<Host.Vo>) = uiThread {
        hostGrid.hosts = hosts
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