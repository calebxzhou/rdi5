package calebxzhou.rdi.ui2.frag

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
            //textButton("衣柜", onClick = { mc go (WardrobeFragment()) })

            textButton("开始-电信", onClick = { start(0) })
            textButton("开始-联通", onClick = { start(1) })
            textButton("开始-移动", onClick = { start(2) })


        }
    }

    fun start(carrier: Int) {
        /* var realCarrier = 0
         //移动只有18~24能用
         val isEvening = LocalTime.now().isAfter(LocalTime.of(18, 0)) && LocalTime.now().isBefore(LocalTime.of(23, 59))
         if (!isEvening && cred.carrier == 2) {
             //其他时间连联通的
             realCarrier = 1
         }*/
        renderThread {

            ConnectScreen.startConnecting(
                mc.screen, mc,

                ServerAddress(server.gameCarrierIp[carrier], server.gamePort), server.mcData, false, null
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