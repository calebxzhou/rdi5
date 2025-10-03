package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.Team
import calebxzhou.rdi.ui2.FragmentSize
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.center
import calebxzhou.rdi.ui2.contextMenu
import calebxzhou.rdi.ui2.headButton
import calebxzhou.rdi.ui2.iconButton
import calebxzhou.rdi.ui2.linearLayout
import calebxzhou.rdi.ui2.textView

class TeamFragment(val team: Team) : RFragment("我的团队") {
    override var fragSize: FragmentSize
        get() = FragmentSize.MEDIUM
        set(value) {}

    init {
        bottomOptionsConfig = {
            "👥 拉人" colored MaterialColor.PINK_800 with {}
            "▶ 游玩主机" colored MaterialColor.GREEN_900 with {}
        }
        contentLayoutInit = {
            iconButton("team", team.name, init = { center() })
            linearLayout {


                textView("成员列表：")
                team.members.forEach { member ->
                    headButton(member.id, init = {
                        setTextColor(
                            when (member.role) {
                                Team.Role.OWNER -> 0xFFFFD700.toInt()
                                Team.Role.ADMIN -> 0xFFC0C0C0.toInt()
                                Team.Role.MEMBER -> 0xFFCD7F32.toInt()
                                else -> -0x1
                            }
                        )
                        contextMenu {
                            if (member.role != Team.Role.OWNER)
                                "踢出" with {}
                            when (member.role) {
                                Team.Role.OWNER -> "解散团队" with {}
                                Team.Role.ADMIN -> "取消管理者身份" with {}
                                Team.Role.MEMBER -> "设置为管理者" with {}
                                else -> {}
                            }
                        }
                    })
                }
            }
        }
    }
}