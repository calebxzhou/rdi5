package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.Room
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.ui2.SELF
import calebxzhou.rdi.ui2.iconButton
import calebxzhou.rdi.ui2.linearLayoutParam
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.renderThread
import icyllis.modernui.view.Gravity
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.multiplayer.resolver.ServerAddress

class RoomFragment(val room: Room) : RFragment("我的房间") {
    val server = RServer.default

    init {
        bottomOptionsConfig = {
            "开玩(电信)" with { start(false) }
            "开玩(电信以外)" with { start(true) }
            "成员" with { }
            "服务端" with { mc go ServerFragment(server) }
            "删除房间" with {
                //confirm("真的要删除整个房间吗？\n所有的存档等内容将永久删除，无法恢复") {
                    mc go ConfirmDeleteRoomFragment(room,server)
               // }
            }
        }
    }

    override fun initContent() {
        contentLayout.apply {
            iconButton("island", room.name, {
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = linearLayoutParam(SELF, SELF) {
                    gravity = Gravity.CENTER_HORIZONTAL
                }
            }) {}
        }
    }

    fun start(bgp: Boolean) {
        renderThread {

            ConnectScreen.startConnecting(
                mc.screen, mc,

                ServerAddress(if (bgp) server.bgpIp else server.ip, server.gamePort), server.mcData(bgp), false, null
            )
        }

    }

}