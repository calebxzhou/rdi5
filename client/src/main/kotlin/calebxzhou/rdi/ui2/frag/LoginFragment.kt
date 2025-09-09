package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.service.PlayerService.playerLogin
import calebxzhou.rdi.ui2.button
import calebxzhou.rdi.ui2.component.REditPassword
import calebxzhou.rdi.ui2.component.RTextField
import calebxzhou.rdi.ui2.fctx
import calebxzhou.rdi.ui2.plusAssign
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.ioScope
import calebxzhou.rdi.util.mc
import kotlinx.coroutines.launch

class LoginFragment : RFragment("登录") {
    private lateinit var qqInput: RTextField
    private lateinit var passwordInput: REditPassword

    override fun initContent() {
        qqInput = RTextField(fctx, "QQ号", icon = "qq").also { contentLayout += it }
        passwordInput = REditPassword(fctx, "密码").also { contentLayout += it }
        contentLayout.apply {   button("登录"){onClicked()}}

    }
    private fun onClicked() {
        val qq = qqInput.edit.text.toString()
        val pwd = passwordInput.text.toString()
        ioScope.launch {
            RServer.now?.playerLogin(this@LoginFragment,qq,pwd)?.let {

                mc go ProfileFragment()
            }
        }

    }
}