package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.Const
import calebxzhou.rdi.PACK
import calebxzhou.rdi.PackMode
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.model.RServer
import calebxzhou.rdi.model.Room
import calebxzhou.rdi.ui2.*
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
            orientation = LinearLayout.VERTICAL

            // Create a container for the head button
            linearLayout {
                layoutParams = linearLayoutParam(PARENT, SELF)
                gravity = Gravity.CENTER

                headButton(account._id)
            }
            textButton("修改信息", onClick = { mc go (ChangeProfileFragment()) })
            textButton("导入正版皮肤", onClick = { mc go (MojangSkinFragment()) })
            textButton("衣柜", onClick = {

                mc go WardrobeFragment()
            }
            )

            textButton("开始-1区电信", onClick = { start(false,0) })
            textButton("开始-1区电信以外", onClick = { start(true, 0) })
            if(PACK == PackMode.SEA)
            {
                textButton("开始-2区电信", onClick = { start(false, 1) })
                textButton("开始-2区电信以外", onClick = { start(true, 1) })
            }
            textButton("退出登录", onClick = { close() })


        }
    }

    fun start(bgp: Boolean, area: Int) {
        renderThread {

            ConnectScreen.startConnecting(
                mc.screen, mc,

                ServerAddress(if(bgp)server.bgpIp else server.ip, server.gamePorts[area]), server.mcData(area,bgp), false, null
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