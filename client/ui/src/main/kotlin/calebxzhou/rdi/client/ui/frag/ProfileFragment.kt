package calebxzhou.rdi.client.ui.frag

import calebxzhou.rdi.client.net.loggedAccount
import calebxzhou.rdi.client.ui.*
import calebxzhou.rdi.client.ui.component.HwSpecView
import calebxzhou.rdi.client.ui2.startUi2

class ProfileFragment : RFragment("我的信息") {
    override var closable = false
    override var fragSize = FragmentSize.MEDIUM

    init {
        bottomOptionsConfig = {
            //"\uEB29 整合包" colored MaterialColor.ORANGE_800 with { ModpackMarketFragment().go() }
            "▶ 大厅" colored MaterialColor.GREEN_900 with { HostLobbyFragment().go()  }
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
                        //WardrobeFragment().go()
                    }
                }
            }
            this += HwSpecView(context).apply { center() }

        }
    }


}