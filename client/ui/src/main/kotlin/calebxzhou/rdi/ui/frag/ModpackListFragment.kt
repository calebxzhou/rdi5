package calebxzhou.rdi.ui.frag

import calebxzhou.rdi.common.model.ModpackVo
import calebxzhou.rdi.common.util.ioTask
import calebxzhou.rdi.net.loggedAccount
import calebxzhou.rdi.net.server
import calebxzhou.rdi.ui.*
import calebxzhou.rdi.ui.component.ModpackGrid
import calebxzhou.rdi.ui.component.alertErr
import kotlin.collections.filter

class ModpackListFragment() : RFragment("浏览整合包") {
    override var fragSize = FragmentSize.FULL

    init {
        contentViewInit = {
            load()
        }
        titleViewInit = {
            textView("选择想玩的整合包及版本，方可创建服务器。如果没有想玩的，可以上传自己的整合包。")
            quickOptions {
                "\uDB81\uDC8B 我的包" make checkbox with {
                    load { it.authorId == loggedAccount._id }
                }
                "\uDB80\uDFD5 上传整合包" colored MaterialColor.BLUE_900 with { ModpackUploadFragment().go() }
                "\uDB86\uDDD8 做新包" colored MaterialColor.AMBER_900 with { alertErr("没写完") }

            }
        }
    }

    private fun load(filter: (ModpackVo) -> Boolean = { true }) = ioTask {
        server.makeRequest<List<ModpackVo>>("modpack").data?.let {
            uiThread {
                contentView.apply {
                    this += ModpackGrid(context, it.filter(filter)) { modpackVo ->
                        ModpackInfoFragment(modpackVo.id).go()
                    }
                }
            }
        }
    }
}