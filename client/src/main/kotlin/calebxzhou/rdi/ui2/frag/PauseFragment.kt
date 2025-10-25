package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.lgr
import calebxzhou.rdi.service.RKeyBinds
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.component.alertOk
import calebxzhou.rdi.util.*
import icyllis.modernui.view.Gravity
import icyllis.modernui.widget.LinearLayout
import net.minecraft.client.gui.screens.GenericMessageScreen
import net.neoforged.neoforge.client.gui.ConfigurationScreen.SAVING_LEVEL
import java.lang.Exception

class PauseFragment : RFragment("暂停") {
    override var fragSize = FragmentSize.SMALL
    override var showBg = false

    init {
        contentLayoutInit = {
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
                        goto(SponsorFragment())

                    }
                    iconButton("mcmod", "MC百科") {
                        alertOk("瞄准/光标放置/手持你想搜索的物品\n按下${RKeyBinds.MCMOD.translatedKeyMessage.string}")
                    }
                }
                linearLayout {
                    gravity = Gravity.CENTER
                    paddingDp(16)
                    /*iconButton("partner","参观"){
                        mc.sendCommand("spec")
                    }*/
                    iconButton("settings", "设置") {
                        goto(SettingsFragment())
                    }
                    iconButton("exit", "退出") {
                        renderThread {
                            mc.level!!.disconnect()
                            mc.level=null
                            if (mc.isLocalServer) {
                                try {
                                    mc.disconnect(TitleFragment().mcScreen)
                                    mc set TitleFragment().mcScreen
                                    mc.singleplayerServer?.let {
                                        it.saveEverything(true, true, true)
                                        it.runningThread.stop()
                                    }
                                } catch (e: Exception) {
                                    lgr.error(e)
                                }

                            } else {
                                mc.disconnect()
                                goto(TeamFragment())
                            }
                        }
                    }
                }
            }
        }
    }
}