package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.RServer
import calebxzhou.rdi.service.PlayerService.playerLogin
import calebxzhou.rdi.ui2.component.REditPassword
import calebxzhou.rdi.ui2.component.REditText
import calebxzhou.rdi.ui2.component.RTextButton
import calebxzhou.rdi.ui2.fctx
import calebxzhou.rdi.ui2.plusAssign
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.ioScope
import calebxzhou.rdi.util.mc
import kotlinx.coroutines.launch

class LoginFragment : RFragment("登录") {
    private lateinit var qqInput: REditText
    private lateinit var passwordInput: REditPassword

    override fun initContent() {
        qqInput = REditText(fctx, "QQ号").also { contentLayout += it }
        passwordInput = REditPassword(fctx, "密码").also { contentLayout += it }
        contentLayout += RTextButton(fctx, "登录", ::onClicked)

    }
    private fun onClicked() {
        val qq = qqInput.text.toString()
        val pwd = passwordInput.text.toString()
        ioScope.launch {
            RServer.now?.playerLogin(qq,pwd)?.let {

                mc go ProfileFragment()
            }
        }

    }
}