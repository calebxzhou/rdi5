package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.body
import calebxzhou.rdi.ui2.button
import calebxzhou.rdi.ui2.component.RButton
import calebxzhou.rdi.ui2.component.REditText
import calebxzhou.rdi.ui2.component.alertOk
import calebxzhou.rdi.ui2.fctx
import calebxzhou.rdi.ui2.plusAssign
import calebxzhou.rdi.ui2.uiThread

class InviteMemberFragment : RFragment("邀请成员") {

    private lateinit var qqInput: REditText
    override fun initContent() {
        qqInput = REditText(fctx, "QQ号").also { contentLayout += it }
        contentLayout.apply {   button("ok"){ onClicked() }}
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