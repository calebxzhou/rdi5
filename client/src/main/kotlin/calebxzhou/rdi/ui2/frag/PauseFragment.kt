package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.net.GameNetClient
import calebxzhou.rdi.service.RKeyBinds
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.lookingAtBlock
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.openAsUri
import icyllis.modernui.view.Gravity
import icyllis.modernui.widget.LinearLayout
import net.minecraft.util.MemoryReserve

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


                    }
                    iconButton("mcmod", "MC百科") {
                        alertOk("鼠标放在你想搜索的物品上\n按下${RKeyBinds.MCMOD.translatedKeyMessage.string}")
                    }
                    iconButton("home", "回家")
                }
                linearLayout {
                    gravity = Gravity.CENTER
                    paddingDp(16)
                    iconButton("partner","参观")
                    iconButton("island","房间中心")
                    iconButton("camera","摄影")
                    iconButton("settings","设置"){
                        mc go SettingsFragment()
                    }
                    iconButton("exit","退出"){
                         GameNetClient.disconnect()
                        try {
                            MemoryReserve.release()
                            mc.levelRenderer.clear()
                            mc.gameRenderer.resetData()
                        } catch (throwable1: Throwable) {
                        }
                        mc.singleplayerServer?.let {
                            it.executeBlocking {
                                it.saveEverything(false, true, true)
                                it.halt(false)
                            }
                            mc.level?.disconnect()
                            mc.gui.onDisconnected()
                            mc.level=null
                            mc.player=null
                            mc.gameMode=null
                        }
                        System.gc()

                        mc go ProfileFragment()


                            //mc.singleplayerServer?.saveEverything(false,true,true)

                            //mc.disconnect(GenericMessageScreen("存档中，请稍候".mcComp))



                        //if (mc.isLocalServer) {
                        // mc.clearLevel(GenericDirtMessageScreen("".mcComp))
                        //}
                    }
0                }
            }
        }
    }
}