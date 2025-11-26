package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.account
import calebxzhou.rdi.model.pack.ModpackVo
import calebxzhou.rdi.net.server
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.component.ModpackGrid
import calebxzhou.rdi.ui2.component.alertErr
import calebxzhou.rdi.util.ioTask

class ModpackListFragment: RFragment("大家的整合包") {
    override var fragSize = FragmentSize.FULL
    init {
        contentViewInit = {
            load()
        }
        titleViewInit = {
            quickOptions {
                "\uDB81\uDC8B 我的包" make checkbox with {
                    load { it.authorId== account._id }
                }
                "\uDB80\uDFD5 上传整合包" colored MaterialColor.BLUE_900 with { ModpackUploadFragment().go() }
                "\uDB86\uDDD8 做新包" colored MaterialColor.AMBER_900 with { alertErr("没写完") }
            }
        }
    }
    private fun load(filter: (ModpackVo) -> Boolean = {true} ) = ioTask {
        server.makeRequest<List<ModpackVo>>("modpack").data?.let {
            uiThread {
                contentView.apply {
                    this += ModpackGrid(context,it.filter(filter)){
                        ModpackInfoFragment(it.id).go()
                    }
                }
            }
        }
    }
}