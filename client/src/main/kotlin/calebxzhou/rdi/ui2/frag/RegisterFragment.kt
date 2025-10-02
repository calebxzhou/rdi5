package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.body
import calebxzhou.rdi.net.success
import calebxzhou.rdi.service.PlayerService
import calebxzhou.rdi.service.playerLogin
import calebxzhou.rdi.ui2.FragmentSize
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.component.REditPassword
import calebxzhou.rdi.ui2.editPwd
import calebxzhou.rdi.ui2.editText
import calebxzhou.rdi.ui2.button
import calebxzhou.rdi.ui2.component.RTextField
import calebxzhou.rdi.ui2.component.alertErr
import calebxzhou.rdi.ui2.component.alertOk
import calebxzhou.rdi.ui2.toast
import calebxzhou.rdi.ui2.uiThread
import calebxzhou.rdi.util.ioScope
import icyllis.modernui.view.Gravity
import kotlinx.coroutines.launch

class RegisterFragment : RFragment("注册新账号") {
    private lateinit var usernameInput: RTextField
    private lateinit var qqInput: RTextField
    private lateinit var passwordInput: RTextField
    private lateinit var passwordInput2: RTextField
    override var fragSize: FragmentSize
        get() = FragmentSize.MEDIUM
        set(value) {}
    override fun initContent() {
        contentLayout.apply {
            this.gravity = Gravity.CENTER_HORIZONTAL
            usernameInput = editText("昵称 支持中文")
            qqInput = editText("QQ号")
            passwordInput = editText("密码"){isPassword=true}
            passwordInput2 = editText("确认密码"){isPassword=true}
            bottomOptionsConfig = {
                "注册" colored MaterialColor.BLUE_800 with {onRegisterClicked()}
            }
        }

    }



    private fun onRegisterClicked() {
        val usr = usernameInput.txt
        val qq = qqInput.txt
        val pwd = passwordInput.txt
        val pwd2 = passwordInput2.txt

        if(pwd != pwd2) {
            alertErr("两次输入的密码不一致")
            return
        }
        RServer.now.hqRequest(true,"register", params =
            listOf("name" to usr, "qq" to qq, "pwd" to pwd) ){
            uiThread {
                toast("注册成功 点击登录开玩")
            }
        }

    }
}
