package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.Room
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.body
import calebxzhou.rdi.ui2.SELF
import calebxzhou.rdi.ui2.bottomOptions
import calebxzhou.rdi.ui2.iconButton
import calebxzhou.rdi.ui2.linearLayoutParam
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.ioScope
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.renderThread
import calebxzhou.rdi.util.serdesJson
import icyllis.modernui.view.Gravity
import kotlinx.coroutines.launch
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.multiplayer.resolver.ServerAddress

class RoomFragment(val room: Room) : RFragment("我的房间") {
    val server = RServer.default

    override fun initContent() {
        contentLayout.apply {
            iconButton("island", room.name, {
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = linearLayoutParam(SELF, SELF) {
                    gravity = Gravity.CENTER_HORIZONTAL
                }
            }) {}
            bottomOptions {
                iconButton("play", "开玩(电信)") { start(false) }
                iconButton("play", "开玩(电信以外)") { start(true) }
                iconButton("smp", "成员") { }
                iconButton("server", "服务端") { mc go ServerFragment(server) }
                iconButton("error", "删除房间") {
                    //confirm("真的要删除整个房间吗？\n所有的存档等内容将永久删除，无法恢复") {
                        mc go ConfirmDeleteRoomFragment(room,server)
                   // }
                }
            }
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