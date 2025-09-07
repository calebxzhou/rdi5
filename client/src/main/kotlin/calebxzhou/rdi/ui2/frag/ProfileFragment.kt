package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.Const
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.model.Room
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.body
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.component.HwSpecView
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.renderThread
import calebxzhou.rdi.util.serdesJson
import icyllis.modernui.view.Gravity
import icyllis.modernui.view.KeyEvent
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.multiplayer.resolver.ServerAddress

class ProfileFragment : RFragment("我的信息") {
    override var closable = false
    val account = RAccount.now ?: RAccount.DEFAULT
    val server = RServer.now ?: RServer.OFFICIAL_DEBUG

    init {
        bottomOptionsConfig = {
            "👚 衣柜" colored MaterialColor.PINK_800 with { mc go WardrobeFragment() }
            "🏠 进入房间" colored MaterialColor.LIGHT_GREEN_900 with {
                server.hqRequest(false, "room/my", false) {
                    val body = it.body
                    if (body == "0") {
                        confirm("你还没有加入房间，你可以：", "创建自己的房间", "等朋友邀请我加入他的",){
                            server.hqRequest(true, "room/create" ){ resp ->
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
            }
            "⛔ 登出" colored MaterialColor.RED_900 with { close() }
        }
    }

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
                this += HwSpecView(context)
            }
            setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEY_1  && event.action == KeyEvent.ACTION_UP) {
                    if(Const.DEBUG)
                    start(false)
                    true
                }
                false
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

        mc go TitleFragment()
    }
}