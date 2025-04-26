package calebxzhou.rdi.ui.screen

import calebxzhou.rdi.model.RServer
import calebxzhou.rdi.serdes.serdesJson
import calebxzhou.rdi.ui.component.RScreen
import calebxzhou.rdi.ui.component.button.RTextButton
import calebxzhou.rdi.ui.layout.linearLayout
import calebxzhou.rdi.util.addChatMessage
import calebxzhou.rdi.util.body
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.mc.mcComp
import calebxzhou.rdi.util.mcMainThread

class RVisitIslandScreen(val server: RServer) : RScreen("选择你想参观的岛屿") {
    override fun doInit() {
        server.hqSendAsync(true, false, "island/list") {
            val islandList = serdesJson.decodeFromString<List<Pair<String, String>>>(it.body)
            mcMainThread {
                linearLayout(this) {
                    startX = 5
                    startY = 20
                    center = false
                    autoWrap = true
                    islandList.map { island ->
                        text(island.second.mcComp) {
                            mc go null
                            server.hqSendAsync(false, true, "island/visit", listOf("iid" to island.first)) {
                                mc.addChatMessage("参观岛屿: ${island.second}. 按下H键回到你自己的岛屿.")
                            }
                        }
                    }
                }
            }
        }
    }
}