package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.component.HwSpecView
import calebxzhou.rdi.ui2.component.alertErr

class ProfileFragment : RFragment("我的信息") {
    override var closable = false
    val account = RAccount.now ?: RAccount.DEFAULT
    val server = RServer.now
    override var fragSize: FragmentSize
        get() = FragmentSize.MEDIUM
        set(value) {}

    init {
        bottomOptionsConfig = {
            //"\uEB29 整合包" colored MaterialColor.ORANGE_800 with { ModpackMarketFragment().go() }
            "👚 衣柜" colored MaterialColor.PINK_800 with { goto(WardrobeFragment()) }
            "▶ 进入团队" colored MaterialColor.GREEN_900 with {
                goto(TeamFragment())
            }
            "\uDB81\uDEEE 信箱" colored MaterialColor.BLUE_900 with { alertErr("没开发完") }
            /*"🏠 团队" colored MaterialColor.LIGHT_GREEN_900 with {
                server.request<String>("room/my", method = io.ktor.http.HttpMethod.Get, showLoading = false) {
                    val body = it.data
                    if (body == "0") {
                        confirm(
                            "你还没有加入房间，你可以：",
                            yesText = "创建自己的房间",
                            noText = "等朋友邀请我加入他的",
                        ) {
                            server.requestU("room/create") { resp ->
                                Room.now= serdesJson.decodeFromString<Room>(resp.data!!)
                                goto(RoomFragment( ))
                            }
                        }
                        return@request
                    } else {
                        Room.now=serdesJson.decodeFromString<Room>(body!!)
                        goto(RoomFragment( ))
                    }
                }
            }*/


        }
        titleViewInit={
            quickOptions {
                "\uDB83\uDFC5 登出" colored MaterialColor.RED_900 with { close() }
            }
        }
        contentViewInit = {
            headButton(account._id, init = {
                center()
            }, onClick = {
                ChangeProfileFragment().showOver(this@ProfileFragment)
            })
            this += HwSpecView(context).apply { center() }

        }
    }


}