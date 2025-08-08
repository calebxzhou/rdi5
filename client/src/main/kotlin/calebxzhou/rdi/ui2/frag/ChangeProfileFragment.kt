package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.model.RServer
import calebxzhou.rdi.ui2.component.REditPassword
import calebxzhou.rdi.ui2.component.REditText
import calebxzhou.rdi.ui2.editPwd
import calebxzhou.rdi.ui2.editText
import calebxzhou.rdi.ui2.textButton

class ChangeProfileFragment: RFragment("修改信息") {
    val account = RAccount.now ?: RAccount.DEFAULT
    private lateinit var nameEdit: REditText
    private lateinit var qqEdit: REditText
    private lateinit var pwdEdit: REditPassword
    override fun initContent() {
        contentLayout.apply {
            nameEdit = editText("昵称") {
                setText(account.name)
            }
            pwdEdit = editPwd("新密码 留空则不修改")
            /*qqEdit = editText("QQ号") {
                setText(account.qq)
            }*/
            textButton("修改"){onChangeClicked()}
        }
    }
    //不允许修改qq
    private fun onChangeClicked() {
        val name = nameEdit.text.toString()
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
        val params = arrayListOf<Pair<String, String>>()
        if (name != account.name) params += "name" to name
        // Only add password to params if it's not empty
        if (pwd.isNotEmpty() && pwd != account.pwd) params += "pwd" to pwd
        //if (qq != account.qq) params + "qq" to qq
        RServer.now?.hqRequest(post=true,path = "profile", params = params) {
            // Use existing password if new password is empty
            val finalPwd = pwd.ifEmpty { account.pwd }
            val newAccount = RAccount(account._id, name, finalPwd, account.qq, account.score, account.cloth)
            RAccount.now = newAccount
            close()
            alertOk("修改成功")
        }

    }
}