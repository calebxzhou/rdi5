package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.auth.LocalCredentials
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
    val cred = LocalCredentials.read()
    var carrier = cred.carrier
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
            radioGroup {
                layoutParams = linearLayoutParam(PARENT, SELF)
                gravity = Gravity.CENTER

            radioButton("电信") {
                isChecked = carrier == 0
                setOnCheckedChangeListener { a, b ->
                    changeCarrier(0)
                }
            }
            radioButton("联通") {
                isChecked = carrier == 1
                setOnCheckedChangeListener { a, b ->
                    changeCarrier(1)
                }
            }
            radioButton("移动") {
                isChecked = carrier == 2
                setOnCheckedChangeListener { a, b ->
                    changeCarrier(2)
                }
            }  }
            textButton("开始", onClick = ::start)


        }
    }

    fun start() {
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

    fun changeCarrier(id: Int) {
        carrier = id
        cred.carrier = id
        cred.write()
    }

    override fun close() {
        RServer.now = null
        RAccount.now = null
        Room.now = null
        mc go TitleFragment()
    }
}