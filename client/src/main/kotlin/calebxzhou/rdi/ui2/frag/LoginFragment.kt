package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.service.PlayerService.playerLogin
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.SELF
import calebxzhou.rdi.ui2.button
import calebxzhou.rdi.ui2.component.RTextField
import calebxzhou.rdi.ui2.dp
import calebxzhou.rdi.ui2.editPwd
import calebxzhou.rdi.ui2.editText
import calebxzhou.rdi.ui2.fctx
import calebxzhou.rdi.ui2.linearLayoutParam
import calebxzhou.rdi.ui2.paddingDp
import calebxzhou.rdi.ui2.plusAssign
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.ioScope
import calebxzhou.rdi.util.mc
import icyllis.modernui.view.Gravity
import kotlinx.coroutines.launch

class LoginFragment : RFragment("登录") {
    private lateinit var qqInput: RTextField
    private lateinit var passwordInput: RTextField

    override fun initContent() {
        contentLayout.apply {
            // Center children
            this.gravity = Gravity.CENTER_HORIZONTAL

            qqInput = editText("QQ号","qq")
            passwordInput = editText("密码", "lock") {
                isPassword = true
            }
            bottomOptionsConfig = {
                "登录" colored MaterialColor.BLUE_800 with {onClicked()}
            }
        }

    }

    private fun onClicked() {
        val qq = qqInput.edit.text.toString()
    val pwd = passwordInput.txt
        ioScope.launch {
            RServer.now?.playerLogin(this@LoginFragment, qq, pwd)?.let {
                mc go ProfileFragment()
            }
        }

    }
}