package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.Room
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.body
import calebxzhou.rdi.ui2.HoldToConfirm.onLongPress
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.SELF
import calebxzhou.rdi.ui2.component.alertErr
import calebxzhou.rdi.ui2.goto
import calebxzhou.rdi.ui2.headButton
import calebxzhou.rdi.ui2.iconButton
import calebxzhou.rdi.ui2.linearLayout
import calebxzhou.rdi.ui2.linearLayoutParam
import calebxzhou.rdi.ui2.paddingDp
import calebxzhou.rdi.ui2.uiThread
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.renderThread
import icyllis.modernui.view.Gravity
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.multiplayer.resolver.ServerAddress

class RoomFragment(val room: Room) : RFragment("我的房间") {
    val server = RServer.now

    init {
        Room.now=room
        bottomOptionsConfig = {
            "▶ 开玩(电信)" colored MaterialColor.GREEN_900 with { start(false) }
            "▶ 开玩(电信以外)" colored MaterialColor.GREEN_700 with { start(true) }
           // "👥 成员" colored MaterialColor.BLUE_500 with { }
            "\uEB50  服务端" colored MaterialColor.BLUE_500 with { goto( ServerFragment( )) }
            //"\uEB29  整合包" colored MaterialColor.YELLOW_800 with { goto( ServerFragment( )) }
            //"\uE6AA  存档" colored MaterialColor.PINK_800 with { goto( ServerFragment( )) }
            "❌ 删除房间" colored MaterialColor.RED_900 init {
                onLongPress(2000){
                    showChildFragmentOver(ConfirmDeleteRoomFragment(room,server))
                }
            } with {
                //confirm("真的要删除整个房间吗？\n所有的存档等内容将永久删除，无法恢复") {

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
            linearLayout {
                gravity = Gravity.CENTER
                paddingDp(0,20,0,0)
                room.members.forEach { headButton(it.id) }
            }
        }
    }

    fun start(bgp: Boolean) {
        server.hqRequest(false,"/room/server/status"){
            if(it.body != "STARTED"){
                alertErr("请先启动房间的服务端")
                return@hqRequest
            }
            renderThread {
                ConnectScreen.startConnecting(
                    mc.screen, mc,
                    ServerAddress(if (bgp) server.bgpIp else server.ip, server.gamePort), server.mcData(bgp), false, null
                )
            }
        }


    }

}