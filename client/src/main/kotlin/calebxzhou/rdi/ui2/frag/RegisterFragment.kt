package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.body
import calebxzhou.rdi.net.success
import calebxzhou.rdi.ui2.component.REditPassword
import calebxzhou.rdi.ui2.component.REditText
import calebxzhou.rdi.ui2.editPwd
import calebxzhou.rdi.ui2.editText
import calebxzhou.rdi.ui2.textButton
import calebxzhou.rdi.util.ioScope
import kotlinx.coroutines.launch

class RegisterFragment : RFragment("注册新账号") {
    private lateinit var usernameInput: REditText
    private lateinit var qqInput: REditText
    private lateinit var passwordInput: REditPassword
    private lateinit var passwordInput2: REditPassword

    override fun initContent() {
        contentLayout.apply { 
            usernameInput = editText("昵称 支持中文")
            qqInput = editText("QQ号")
            passwordInput = editPwd("密码")
            passwordInput2 = editPwd("确认密码")
            textButton("注册"){onRegisterClicked()}
        }

    }



    private fun onRegisterClicked() {
        val usr = usernameInput.text.toString()
        val qq = qqInput.text.toString()
        val pwd = passwordInput.text.toString()
        val pwd2 = passwordInput2.text.toString()
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

        ioScope.launch {
            RServer.now?.prepareRequest(true,"register",
                listOf("name" to usr, "qq" to qq, "pwd" to pwd)
            )?.let { resp ->
                    if (resp.success) {
                        close()
                        alertOk("注册成功")
                    }else{
                        alertErr(resp.body)
                    }
            }
        }
    }
}
