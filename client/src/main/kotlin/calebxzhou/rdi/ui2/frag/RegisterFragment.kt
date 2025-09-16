package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.body
import calebxzhou.rdi.net.success
import calebxzhou.rdi.service.PlayerService
import calebxzhou.rdi.service.playerLogin
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

    override fun initContent() {
        contentLayout.apply {
            this.gravity = Gravity.CENTER_HORIZONTAL
            usernameInput = editText("昵称 支持中文","ssp")
            qqInput = editText("QQ号","qq")
            passwordInput = editText("密码","lock"){isPassword=true}
            passwordInput2 = editText("确认密码","lock"){isPassword=true}
            bottomOptionsConfig = {
                "注册" colored MaterialColor.BLUE_800 with {onRegisterClicked()}
            }
        }

    }



    private fun onRegisterClicked() {
        val usr = usernameInput.txt.toString()
        val qq = qqInput.txt.toString()
        val pwd = passwordInput.txt.toString()
        val pwd2 = passwordInput2.txt.toString()
        val usrSize = usr.toByteArray(Charsets.UTF_8).size
        if(usrSize !in 3..24){
            alertErr("昵称须在3~24个字节，当前为$usrSize（1个汉字=3字节）")
            return
        }
        if(qq.length !in 5..10 || !qq.all { it.isDigit() }) {
            alertErr("QQ号格式不正确")
            return
        }
        if(pwd.length !in 6..16) {
            alertErr("密码长度须在6~16个字符")
            return
        }
        if(pwd != pwd2) {
            alertErr("两次输入的密码不一致")
            return
        }
        RServer.now.hqRequest(true,"register", params =
            listOf("name" to usr, "qq" to qq, "pwd" to pwd) ){
            uiThread {
                playerLogin(qq,pwd)
                toast("注册成功 点击登录开玩")
            }
        }

    }
}
