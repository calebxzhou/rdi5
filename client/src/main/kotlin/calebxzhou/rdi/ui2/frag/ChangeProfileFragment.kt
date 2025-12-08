package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.net.server
import calebxzhou.rdi.ui2.FragmentSize
import calebxzhou.rdi.ui2.button
import calebxzhou.rdi.ui2.component.RTextField
import calebxzhou.rdi.ui2.component.alertErr
import calebxzhou.rdi.ui2.component.alertOk
import calebxzhou.rdi.ui2.textField
import io.ktor.http.*

class ChangeProfileFragment: RFragment("修改信息") {
    val account = RAccount.now ?: RAccount.DEFAULT
    override var fragSize: FragmentSize
        get() = FragmentSize.SMALL
        set(value) {}
    private lateinit var nameEdit: RTextField
    private lateinit var pwdEdit: RTextField
    init {
        contentViewInit = {
            nameEdit = textField("昵称") {
                text = account.name
            }
            pwdEdit = textField("新密码 留空则不修改"){isPassword=true}
            /*qqEdit = editText("QQ号") {
                text = account.qq
            }*/
            button("修改"){onChangeClicked()}
        }
    }
    //不允许修改qq
    private fun onChangeClicked() {
        val name = nameEdit.txt.toString()
        val pwd = pwdEdit.text.toString()
        if (name.toByteArray().size !in 3..24) {
            alertErr("昵称须在3~24个字节，当前为${name.toByteArray(Charsets.UTF_8).size}（1个汉字=3字节）")
            return
        }
        /*if(qq.length !in 5..10 || !qq.all { it.isDigit() }) {
            alertErr("QQ号格式不正确")
            return
        }*/
        // Only validate password if it's not empty (empty means no change intended)
        if(pwd.isNotEmpty() && pwd.length !in 6..16) {
            alertErr("密码长度须在6~16个字符")
            return
        }
        val params = mutableMapOf<String, Any>()
        if (name != account.name) params["name"] = name
        // Only add password to params if it's not empty
        if (pwd.isNotEmpty() && pwd != account.pwd) params["pwd"] = pwd
        //if (qq != account.qq) params["qq"] = qq
        server.requestU("player/profile", params = params, method = HttpMethod.Put) {
            // Use existing password if new password is empty
            val finalPwd = pwd.ifEmpty { account.pwd }
            val newAccount = RAccount(account._id, name, finalPwd, account.qq, account.score, account.cloth)
            RAccount.now = newAccount
            close()
            alertOk("修改成功")
        }

    }
}