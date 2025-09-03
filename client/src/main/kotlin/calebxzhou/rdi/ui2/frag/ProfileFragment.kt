package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.Const
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.model.Room
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.body
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.component.HwSpecView
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.ioScope
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.renderThread
import calebxzhou.rdi.util.serdesJson
import icyllis.modernui.view.Gravity
import icyllis.modernui.widget.LinearLayout
import kotlinx.coroutines.launch
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.multiplayer.resolver.ServerAddress

class ProfileFragment : RFragment("我的信息") {
    override var closable = false
    val account = RAccount.now ?: RAccount.DEFAULT
    val server = RServer.now ?: RServer.OFFICIAL_DEBUG
    override fun initContent() {
        contentLayout.apply {
            headButton(account._id, {
                gravity = Gravity.CENTER_HORIZONTAL
                layoutParams = linearLayoutParam(SELF, SELF) {
                    gravity = Gravity.CENTER_HORIZONTAL
                }
            }) {
                mc go ChangeProfileFragment()
            }
            linearLayout {

                layoutParams = linearLayoutParam(SELF, SELF) {
                    gravity = Gravity.CENTER_HORIZONTAL
                }


                this += HwSpecView(context).apply {

                }
            }
            frameLayout {
                layoutParams = frameLayoutParam(PARENT, PARENT)
                bottomOptions {

                    iconButton("clothes", "衣柜", onClick = { mc go WardrobeFragment() })
                    iconButton("island", "进入房间", onClick = {

                        server.hqRequest(false, "room/my", false) {
                            val body = it.body
                            if (body == "0") {
                                confirm("你还没有加入房间，你可以：", "创建自己的房间", "等朋友邀请我加入他的",){
                                    server.hqRequest(true, "room/create"){ resp ->
                                        val room = serdesJson.decodeFromString<Room>(resp.body)
                                        mc go RoomFragment(room)
                                    }
                                }
                                return@hqRequest
                            } else {
                                val room = serdesJson.decodeFromString<Room>(body)
                                mc go RoomFragment(room)
                            }
                        }

                        // mc go RoomFragment()
                    })
                    //快速连接本地服务器 测试用
                    if (Const.DEBUG)
                        iconButton("play", "开始", onClick = { start(false) })
                    iconButton("error", "登出", onClick = { close() })
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


    override fun close() {
        RServer.now = null
        RAccount.now = null
        Room.now = null
        mc go TitleFragment()
    }
}