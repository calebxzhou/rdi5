package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.model.Room
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.component.HwSpecView
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.renderThread
import icyllis.modernui.view.Gravity
import icyllis.modernui.widget.LinearLayout
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.multiplayer.resolver.ServerAddress

class ProfileFragment : RFragment("我的信息") {
    override var closable = false
    val account = RAccount.now ?: RAccount.DEFAULT
    val server = RServer.now ?: RServer.OFFICIAL_DEBUG
    override fun initContent() {
        contentLayout.apply {
            headButton(account._id,{
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
                    iconButton("play", "开始-电信", onClick = { start(false) })
                    iconButton("play", "开始-电信以外", onClick = { start(true) })
                    iconButton("error", "退出登录", onClick = { close() })
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