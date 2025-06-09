package calebxzhou.rdi.ui.screen

import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.body
import calebxzhou.rdi.serdes.serdesJson
import calebxzhou.rdi.ui.layout.linearLayout
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.mcComp
import calebxzhou.rdi.util.renderThread

class RVisitIslandScreen(val server: RServer) : RScreen("选择你想参观的房间") {
    override fun doInit() {
        server.hqRequest(path = "island/list") {


            val islandList = serdesJson.decodeFromString<List<Pair<String, String>>>(it.body)
            renderThread {
                linearLayout(this@RVisitIslandScreen) {
                    startX = 5
                    startY = 20
                    center = false
                    autoWrap = true
                    islandList.map { island ->
                        text(island.second.mcComp) {
                            mc go null
                            /*server.hqSendAsync(false, true, "island/visit", listOf("iid" to island.first)) {
                                mc.addChatMessage("参观房间: ${island.second}. 按下H键回到你自己的房间.")
                            }*/
                        }
                    }
                }
            }

        }
    }
}