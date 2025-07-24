package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.auth.LocalCredentials
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.body
import calebxzhou.rdi.ui.screen.RProfileScreen
import calebxzhou.rdi.ui2.component.REditPassword
import calebxzhou.rdi.ui2.component.REditText
import calebxzhou.rdi.ui2.fctx
import calebxzhou.rdi.ui2.plusAssign
import calebxzhou.rdi.ui2.textButton
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.serdesJson
import kotlin.collections.plusAssign

class LoginFragment : RFragment("登录") {
    private lateinit var qqInput: REditText
    private lateinit var passwordInput: REditPassword

    override fun initContent() {
        qqInput = REditText(fctx, "QQ号").also { contentLayout += it }
        passwordInput = REditPassword(fctx, "密码").also { contentLayout += it }
        contentLayout += textButton(fctx, "登录", ::onClicked)

    }
    private fun onClicked() {
        val qq = qqInput.text.toString()
        val pwd = passwordInput.text.toString()
        RServer.now?.hqRequest(
            path = "login",
            post = true,
            params = listOf("qq" to qq, "pwd" to pwd),
            onOk = {
                val account = serdesJson.decodeFromString<RAccount>(it.body)
                val creds = LocalCredentials.read()
                creds.idPwds += account._id.toHexString() to account.pwd
                creds.lastLoggedId = account._id.toHexString()
                creds.write()
                RAccount.now = account
                mc go RProfileScreen()
            }
        )
    }
}