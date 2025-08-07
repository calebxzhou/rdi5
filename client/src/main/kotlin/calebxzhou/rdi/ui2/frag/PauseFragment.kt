package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.net.GameNetClient
import calebxzhou.rdi.service.RKeyBinds
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.util.*
import icyllis.modernui.view.Gravity
import icyllis.modernui.widget.LinearLayout

class PauseFragment : RFragment("暂停") {
    val lookingBlockState = mc.player?.lookingAtBlock

    override fun initContent() {
        contentLayout.apply {
            linearLayout {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = linearLayoutParam(PARENT, PARENT)
                linearLayout {
                    gravity = Gravity.CENTER
                    paddingDp(16)


                    // First row - 3 buttons
                    iconButton("server", "顶服") {
                        "https://play.mcmod.cn/sv20188037.html".openAsUri()
                    }
                    iconButton("heart", "赞助") {
                        mc go SponsorFragment()

                    }
                    iconButton("mcmod", "MC百科") {
                        alertOk("瞄准/光标放置/手持你想搜索的物品\n按下${RKeyBinds.MCMOD.translatedKeyMessage.string}")
                    }
                    iconButton("home", "回家")
                }
                linearLayout {
                    gravity = Gravity.CENTER
                    paddingDp(16)
                    iconButton("partner","参观"){
                        mc.sendCommand("spec")
                    }
                    iconButton("island","房间中心")
                    iconButton("camera","摄影")
                    iconButton("settings","设置"){
                        mc go SettingsFragment()
                    }
                    iconButton("exit","退出"){
                        renderThread {

                            GameNetClient.disconnect()
                            mc.level?.disconnect()
                            mc.disconnect( )
                            mc go ProfileFragment()
                        }
                    }
0                }
            }
        }
    }
}