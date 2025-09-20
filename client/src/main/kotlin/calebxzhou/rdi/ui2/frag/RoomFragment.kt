package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.auth.LocalCredentials
import calebxzhou.rdi.model.Room
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.body
import calebxzhou.rdi.ui2.HoldToConfirm.onLongPress
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.SELF
import calebxzhou.rdi.ui2.center
import calebxzhou.rdi.ui2.component.alertErr
import calebxzhou.rdi.ui2.contextMenu
import calebxzhou.rdi.ui2.goto
import calebxzhou.rdi.ui2.headButton
import calebxzhou.rdi.ui2.iconButton
import calebxzhou.rdi.ui2.linearLayout
import calebxzhou.rdi.ui2.linearLayoutParam
import calebxzhou.rdi.ui2.paddingDp
import calebxzhou.rdi.ui2.showOver
import calebxzhou.rdi.ui2.uiThread
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.renderThread
import icyllis.modernui.view.Gravity
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.multiplayer.resolver.ServerAddress

class RoomFragment() : RFragment("我的房间") {
    val server = RServer.now
    val room = Room.now?:Room.DEFAULT
    init {
        bottomOptionsConfig = {
            "▶ 开玩" colored MaterialColor.GREEN_900 init {
                contextMenu{
                    "\uEF09 选择节点" with { CarrierFragment().showOver(this@RoomFragment) }
                }
            } with { start() }
            "\uEB50  管理" colored MaterialColor.BLUE_500 with { goto( ServerFragment( )) }
            "❌ 删除房间" colored MaterialColor.RED_900 init {
                onLongPress(2000){
                    ConfirmDeleteRoomFragment( ).showOver(this@RoomFragment)
                }
            } with {}
           // "👥 成员" colored MaterialColor.BLUE_500 with { }

            //"\uEB29  整合包" colored MaterialColor.YELLOW_800 with { goto( ServerFragment( )) }
            //"\uE6AA  存档" colored MaterialColor.PINK_800 with { goto( ServerFragment( )) }

        }
        contentLayoutInit={
            iconButton("island", room.name, {
                center()
            })
            linearLayout {
                center()
                paddingDp(0,20,0,0)
                room.members.forEach { headButton(it.id) }
            }
        }
    }


    fun start( ) {
        //电信以外全bgp
        val bgp = LocalCredentials.read().carrier!=0
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