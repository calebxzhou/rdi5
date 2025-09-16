package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.service.playerLogin
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.component.RTextField
import calebxzhou.rdi.ui2.editText
import calebxzhou.rdi.util.ioScope
import icyllis.modernui.view.Gravity
import kotlinx.coroutines.launch

class LoginFragment : RFragment("登录") {
    private lateinit var qqInput: RTextField
    private lateinit var passwordInput: RTextField

    override fun initContent() {
        contentLayout.apply {
            // Center children
            this.gravity = Gravity.CENTER_HORIZONTAL

            qqInput = editText("QQ号", "qq")
            passwordInput = editText("密码", "lock") {
                isPassword = true
            }
            bottomOptionsConfig = {
                "登录" colored MaterialColor.BLUE_800 with { onClicked() }
            }
        }

    }

    private fun onClicked() {
        val qq = qqInput.edit.text.toString()
        val pwd = passwordInput.txt
        ioScope.launch {
            playerLogin(qq, pwd)

        }

    }
}