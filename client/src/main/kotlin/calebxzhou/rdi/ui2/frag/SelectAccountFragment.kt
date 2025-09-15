package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.auth.LocalCredentials
import calebxzhou.rdi.service.playerLogin
import calebxzhou.rdi.ui2.*
import icyllis.modernui.view.Gravity
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

                    creds.loginInfos.forEach { (id, pwd) ->
                        headButton(ObjectId(id), onClick = {
                                playerLogin(id, pwd)
                        }, init = {
                            layoutParams = linearLayoutParam(SELF, SELF)
                            gravity = Gravity.CENTER_HORIZONTAL
                            if (creds.lastLogged?.id == id) {
                                setTextColor(MaterialColor.YELLOW_800.colorValue)
                            }
                           /* contextMenu(listOf(
                                "删除账号" to { deleteAccount(id) }
                            ))*/
                        })
                    }
                }

        }

    }
  /*  fun deleteAccount(id: String) {
        creds.idPwds.remove(id)
        creds.write()
        contentLayout.apply {
            removeAllViews()
            initContent()
        }
    }*/
}