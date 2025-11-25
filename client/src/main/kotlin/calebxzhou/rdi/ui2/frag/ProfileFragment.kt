package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.model.account
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.component.HwSpecView
import calebxzhou.rdi.ui2.component.alertErr

class ProfileFragment : RFragment("我的信息") {
    override var closable = false
    override var fragSize = FragmentSize.MEDIUM

    init {
        bottomOptionsConfig = {
            //"\uEB29 整合包" colored MaterialColor.ORANGE_800 with { ModpackMarketFragment().go() }
            "▶ 服务器大厅" colored MaterialColor.GREEN_900 with {
                HostLobbyFragment().go()
            }
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
                "\uEB1C 信箱" colored MaterialColor.BLUE_900 with { MailFragment().go() }
                "\uDB83\uDFC5 登出" colored MaterialColor.RED_900 with { close() }
            }
        }
        contentViewInit = {
            linearLayout {
                center()
                headButton(account._id)
                textView("\uEB51"){
                    setOnClickListener {
                        ChangeProfileFragment().go()
                    }
                }
                textView("    ")
                textView ("\uEE1C"){
                    setOnClickListener {
                        WardrobeFragment().go()
                    }
                }
            }
            this += HwSpecView(context).apply { center() }

        }
    }


}