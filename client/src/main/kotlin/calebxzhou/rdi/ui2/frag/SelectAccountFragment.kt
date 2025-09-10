package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.auth.LocalCredentials
import calebxzhou.rdi.common.GREEN
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.service.PlayerService.playerLogin
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.ioScope
import calebxzhou.rdi.util.mc
import icyllis.modernui.view.Gravity
import icyllis.modernui.widget.LinearLayout
import kotlinx.coroutines.launch
import org.bson.types.ObjectId

class SelectAccountFragment(val server: RServer) : RFragment("选择账号") {
    val creds = LocalCredentials.read()
    //否则登录账号回来看不见
    override var contentViewCache = false

    init {
        RServer.now = server

        bottomOptionsConfig = {
            "➕ 添加旧号" with {showChildFragmentOver( LoginFragment()) }
            "✏ 注册新号" colored MaterialColor.LIGHT_GREEN_900 with { showChildFragmentOver(RegisterFragment()) }
            "⚙ 设置" colored MaterialColor.BLUE_900 with { mc go SettingsFragment() }
            /*"自由创造" with {
                renderThread {
                    LevelService.openFlatLevel()
                }
            }*/
        }
    }

    override fun close() {
        mc go TitleFragment()
    }

    override fun initContent() {
        contentLayout.apply {
                layoutParams = frameLayoutParam(PARENT, PARENT)

                linearLayout {
                    gravity = Gravity.CENTER_HORIZONTAL
                    orientation = LinearLayout.VERTICAL
                    layoutParams = frameLayoutParam(PARENT, PARENT)

                    creds.idPwds.forEach { (id, pwd) ->
                        headButton(ObjectId(id), onClick = {
                            RServer.now = server
                            ioScope.launch {
                                RServer.now?.playerLogin(this@SelectAccountFragment,id, pwd)?.let {
                                    mc go ProfileFragment()
                                }
                            }
                        }, init = {
                            layoutParams = linearLayoutParam(SELF, SELF)
                            gravity = Gravity.CENTER_HORIZONTAL
                            if (creds.lastLoggedId == id) {
                                setTextColor(GREEN)
                            }
                            contextMenu(listOf(
                                "删除账号" to { deleteAccount(id) }
                            ))
                        })
                    }
                }

        }

    }
    fun deleteAccount(id: String) {
        creds.idPwds.remove(id)
        creds.write()
        contentLayout.apply {
            removeAllViews()
            initContent()
        }
    }
}