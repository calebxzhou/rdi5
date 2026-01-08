package calebxzhou.rdi.ui.frag

import calebxzhou.rdi.common.model.McVersion
import calebxzhou.rdi.model.firstLoaderDir
import calebxzhou.rdi.net.loggedAccount
import calebxzhou.rdi.ui.*
import calebxzhou.rdi.ui.component.HwSpecView
import calebxzhou.rdi.ui.component.alertErr

class ProfileFragment : RFragment("我的信息") {
    override var closable = false
    override var fragSize = FragmentSize.MEDIUM

    init {
        bottomOptionsConfig = {
            //"\uEB29 整合包" colored MaterialColor.ORANGE_800 with { ModpackMarketFragment().go() }
            "▶ 大厅" colored MaterialColor.GREEN_900 with {
                if (McVersion.V211.firstLoaderDir.exists()) {
                    HostLobbyFragment().go()
                    return@with
                }
                alertErr("你还没有完整下载mc文件，必须下载才能游玩RDI，可以去群文件下载“资源运行库包”，也可以点击上方“MC版本资源”按钮下载")
            }
            /*"🏠 团队" colored MaterialColor.LIGHT_GREEN_900 with {
                server.request<String>("room/my", method = io.ktor.http.HttpMethod.Get, showLoading = false) {
                    val body = it.data
                    if (body == "0") {
                        confirm(
                            "你还没有加入主机，你可以：",
                            yesText = "创建自己的主机",
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
        titleViewInit = {
            quickOptions {
                "\uDB84\uDE5F MC版本资源" with { McVersionManageFragment().go() }
                "\uEB1C 信箱" colored MaterialColor.BLUE_900 with { MailFragment().go() }
                "\uDB83\uDFC5 登出" colored MaterialColor.RED_900 with { close() }
            }
        }
        contentViewInit = {
            linearLayout {
                center()
                headButton(loggedAccount._id)
                textView("\uEB51") {
                    setOnClickListener {
                        ChangeProfileFragment().go()
                    }
                }
                textView("    ")
                textView("\uEE1C") {
                    setOnClickListener {
                        WardrobeFragment().go()
                    }
                }
            }
            this += HwSpecView(context).apply { center() }

        }
    }


}