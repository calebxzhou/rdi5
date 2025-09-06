package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.model.Room
import calebxzhou.rdi.ui2.headButton
import calebxzhou.rdi.ui2.button
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.mc

class RoomCenterFragment(val account: RAccount, val server: RServer,val room: Room): RFragment("房间中心") {
    override fun initContent() {
        contentLayout.apply {

            room.members.forEach { headButton(it.id) }
            button("邀请成员"){mc go InviteMemberFragment() }
        }
    }
}