package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.Host
import calebxzhou.rdi.model.pack.Mod
import calebxzhou.rdi.net.server
import calebxzhou.rdi.service.ModService
import calebxzhou.rdi.service.ModService.Companion.toVo
import calebxzhou.rdi.ui2.FragmentSize
import calebxzhou.rdi.ui2.component.ModGrid
import calebxzhou.rdi.ui2.textView
import calebxzhou.rdi.util.ioTask
import org.bson.types.ObjectId

class HostModFragment(val host: Host): RFragment("主机的所有Mod") {
    override var fragSize = FragmentSize.FULL
    private lateinit var extraModGrid: ModGrid
    private lateinit var packModGrid: ModGrid
    init {

        contentViewInit = {
            textView("附加Mod：")
            textView("整合包Mod：")
            extraModGrid = ModGrid(context)
            packModGrid = ModGrid(context)
            loadPackMods()
            loadExtraMods()
        }
        titleViewInit = {
            "+ 添加Mod"
        }
    }
    private fun loadPackMods() = ioTask {
        val mods = server.makeRequest<List<Mod>>(path = "modpack/${host.packId}/${host.packVer}/mods").data!!
        val ms = ModService()
        mods.mapNotNull { ms.cfSlugBriefInfo[it.slug] }.map { it.toVo() }.let { packModGrid.showMods(it) }
    }
    private fun loadExtraMods() = ioTask {
        val ms = ModService()
        host.extraMods.mapNotNull { ms.cfSlugBriefInfo[it.slug] }.map { it.toVo() }.let { packModGrid.showMods(it) }
    }
}