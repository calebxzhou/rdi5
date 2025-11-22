package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.Host
import calebxzhou.rdi.model.pack.ModpackDetailedVo
import calebxzhou.rdi.model.pack.latest
import calebxzhou.rdi.net.server
import calebxzhou.rdi.service.CurseForgeService.fillCurseForgeVo
import calebxzhou.rdi.ui2.FragmentSize
import calebxzhou.rdi.ui2.headButton
import calebxzhou.rdi.ui2.linearLayout
import calebxzhou.rdi.ui2.textView
import calebxzhou.rdi.ui2.uiThread
import org.bson.types.ObjectId

class HostInfoFragment(val hostId: ObjectId): RFragment("主机详细信息") {
    override var fragSize = FragmentSize.FULL
    override var preserveViewStateOnDetach =true
    init {

        contentViewInit = {
            server.request<Host>("host/$hostId") { host ->

                host.data?.run {
                    load()
                }
            }
        }
    }
    private fun Host.load() = uiThread{
        contentView.apply {
            textView("名称：${name}")
            textView("简介：${intro}")
            linearLayout {
                textView("服主：")
                headButton(ownerId)
            }

        }
    }
}