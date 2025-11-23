package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.net.server
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.component.REditText
import org.bson.types.ObjectId

class HostInviteMemberFragment(val hostId: ObjectId) : RFragment("邀请成员") {
    override var fragSize = FragmentSize.SMALL

    private lateinit var qqInput: REditText

    init {
        contentViewInit = {
            qqInput = REditText(fctx, "对方QQ号").also { contentView += it }
            quickOptions {
                "邀请" colored MaterialColor.GREEN_900 with {
                    val qq = qqInput.text
                    server.requestU("host/${hostId}/member/$qq") {
                        close()
                        toast("拉人成功")
                    }
                }
            }
        }
    }
}