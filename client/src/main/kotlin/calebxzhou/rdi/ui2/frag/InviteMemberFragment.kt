package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.RServer
import calebxzhou.rdi.net.body
import calebxzhou.rdi.ui2.component.REditText
import calebxzhou.rdi.ui2.component.RTextButton
import calebxzhou.rdi.ui2.fctx
import calebxzhou.rdi.ui2.plusAssign
import calebxzhou.rdi.util.uiThread

class InviteMemberFragment : RFragment("邀请成员") {

    private lateinit var qqInput: REditText
    override fun initContent() {
        qqInput = REditText(fctx, "QQ号").also { contentLayout += it }
        contentLayout += RTextButton(fctx, "ok"){onClicked()}
    }
    private fun onClicked() {
        val qq = qqInput.text.toString()
            RServer.now?.hqRequest(true,"room/invite_qq", params = listOf("qq" to qq)) {
                uiThread {

                    close()
                    alertOk("成功邀请${it.body}加入了你的房间。")
                }
            }


    }
}