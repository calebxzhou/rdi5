package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.auth.LocalCredentials
import calebxzhou.rdi.auth.LoginInfo
import calebxzhou.rdi.service.playerLogin
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.component.confirm
import icyllis.modernui.view.Gravity
import icyllis.modernui.view.View
import icyllis.modernui.view.ViewGroup
import icyllis.modernui.widget.LinearLayout
import org.bson.types.ObjectId

class SelectAccountFragment() : RFragment("选择账号") {
    val creds = LocalCredentials.read()
    //否则登录账号回来看不见
    override var contentViewCache = false

    init {
        bottomOptionsConfig = {
            "➕ 添加旧号" with {showChildFragmentOver( LoginFragment()) }
            "✏ 注册新号" colored MaterialColor.LIGHT_GREEN_900 with { showChildFragmentOver(RegisterFragment()) }
            "⚙ 设置" colored MaterialColor.BLUE_900 with { goto(SettingsFragment()) }
            /*"自由创造" with {
                renderThread {
                    LevelService.openFlatLevel()
                }
            }*/
        }
    }


    override fun initContent() {
        contentLayout.apply {
                layoutParams = frameLayoutParam(PARENT, PARENT)

                linearLayout {
                    gravity = Gravity.CENTER_HORIZONTAL
                    orientation = LinearLayout.VERTICAL
                    layoutParams = frameLayoutParam(PARENT, PARENT)
                    if(creds.loginInfos.isEmpty()){
                        confirm("当前没有可用账号，是否前往注册？"){
                            showChildFragmentOver(RegisterFragment())
                        }
                    }
                    creds.loginInfos.forEach { info ->
                        headButton(info.key, onClick = {
                                playerLogin(info.key.toHexString(),info.value.pwd)
                        }, init = {
                            // Use the key's timestamp so we can reliably find and remove this view later
                            id = info.key.timestamp
                            layoutParams = linearLayoutParam(SELF, SELF)
                            gravity = Gravity.CENTER_HORIZONTAL
                            if (creds.lastLogged?.id == uid) {
                                setTextColor(MaterialColor.YELLOW_800.colorValue)
                            }
                            contextMenu{
                                "❌ 删除此记录" with  { deleteAccount(info.key) }
                            }
                        })
                    }
                }

        }

    }
    fun deleteAccount(info: ObjectId) {
        creds.loginInfos.remove(info)
        creds.write()
        contentLayout.findViewById<View>(info.timestamp)?.let { v ->
            // Remove from its actual parent, not necessarily the root container
            (v.parent as? ViewGroup)?.removeView(v)
        }
        toast("已删除记录")
    }
}