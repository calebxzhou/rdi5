package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.pack.Modpack
import calebxzhou.rdi.model.pack.ModpackInfo
import calebxzhou.rdi.net.server
import calebxzhou.rdi.ui2.FragmentSize
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.component.ModpackGrid
import calebxzhou.rdi.ui2.component.alertErr
import calebxzhou.rdi.ui2.frag.pack.ModpackCreateFragment
import calebxzhou.rdi.ui2.go
import calebxzhou.rdi.ui2.plusAssign
import calebxzhou.rdi.ui2.uiThread
import calebxzhou.rdi.util.ioTask

class ModpackListFragment: RFragment("大家的整合包") {
    override var fragSize = FragmentSize.FULL
    init {
        contentViewInit = {
            load()
        }
        titleViewInit = {
            quickOptions {
                "\uDB80\uDFD5 传包" colored MaterialColor.BLUE_900 with { ModpackCreateFragment().go() }
                "\uEB29 我的包" colored MaterialColor.TEAL_900 with { ModpackCreateFragment().go() }
                "\uDB86\uDDD8 做新包" colored MaterialColor.AMBER_900 with { alertErr("没写完") }
            }
        }
    }
    private fun load() = ioTask {
        server.makeRequest<List<ModpackInfo>>("modpack").data?.let {
            uiThread {
                contentView.apply {
                    this += ModpackGrid(context,it){
                        HostListFragment.Create(it).go()
                    }
                }
            }
        }
    }
}