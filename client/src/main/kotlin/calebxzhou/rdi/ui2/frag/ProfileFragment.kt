package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.Const
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.model.Team
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.component.HwSpecView
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.renderThread
import icyllis.modernui.view.KeyEvent
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.multiplayer.resolver.ServerAddress

class ProfileFragment : RFragment("我的信息") {
    override var closable = false
    val account = RAccount.now ?: RAccount.DEFAULT
    val server = RServer.now
    override var fragSize: FragmentSize
        get() = FragmentSize.MEDIUM
        set(value) {}
    init {
        bottomOptionsConfig = {
            "👚 衣柜" colored MaterialColor.PINK_800 with { goto(WardrobeFragment()) }
            "▶ 进入团队" colored MaterialColor.GREEN_900 with {
                server.hqRequestT<Team>(false,"team/my", false) { resp ->
                    resp.data?.let { TeamFragment(it).go() }
                }
            }
            /*"🏠 团队" colored MaterialColor.LIGHT_GREEN_900 with {
                server.hqRequest(false, "room/my", false) {
                    val body = it.data
                    if (body == "0") {
                        confirm(
                            "你还没有加入房间，你可以：",
                            yesText = "创建自己的房间",
                            noText = "等朋友邀请我加入他的",
                        ) {
                            server.hqRequest(true, "room/create") { resp ->
                                Room.now= serdesJson.decodeFromString<Room>(resp.data)
                                goto(RoomFragment( ))
                            }
                        }
                        return@hqRequest
                    } else {
                        Room.now=serdesJson.decodeFromString<Room>(body)
                        goto(RoomFragment( ))
                    }
                }
            }*/
            "⛔ 登出" colored MaterialColor.RED_900 with { close() }
        }
        contentLayoutInit = {
            headButton(account._id, init = {
                center()
            }, onClick = {
                ChangeProfileFragment().showOver(this@ProfileFragment)
            })
            this += HwSpecView(context).apply { center() }
            keyAction {
                KeyEvent.KEY_1 to {
                    if (Const.DEBUG)
                        start(false)
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


}