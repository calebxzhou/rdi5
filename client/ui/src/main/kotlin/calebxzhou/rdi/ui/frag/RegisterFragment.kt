package calebxzhou.rdi.ui.frag

import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.ui.*
import calebxzhou.rdi.ui.component.RTextField
import calebxzhou.rdi.ui.component.alertErr
import icyllis.modernui.view.Gravity

class RegisterFragment : RFragment("注册新账号") {
    private lateinit var usernameInput: RTextField
    private lateinit var qqInput: RTextField
    private lateinit var passwordInput: RTextField
    private lateinit var passwordInput2: RTextField
    override var fragSize: FragmentSize
        get() = FragmentSize.MEDIUM
        set(value) {}
    init {
        contentViewInit = {
            gravity = Gravity.CENTER_HORIZONTAL
            usernameInput = textField("昵称 支持中文")
            qqInput = textField("QQ号")
            passwordInput = textField("密码"){isPassword=true}
            passwordInput2 = textField("确认密码"){isPassword=true}
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
        RServer.now.requestU("player/register", params =
            mapOf("name" to usr, "qq" to qq, "pwd" to pwd) ){
            uiThread {
                toast("注册成功 点击登录开玩")
            }
        }

    }
}
