package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.Team
import calebxzhou.rdi.ui2.FragmentSize
import calebxzhou.rdi.ui2.headButton
import calebxzhou.rdi.ui2.iconButton

class TeamFragment(val team: Team): RFragment("我的团队") {
    override var fragSize: FragmentSize
        get() = FragmentSize.MEDIUM
        set(value) {}

    init {
        contentLayoutInit = {
            iconButton("team", team.name)
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
                })
            }
        }
    }
}