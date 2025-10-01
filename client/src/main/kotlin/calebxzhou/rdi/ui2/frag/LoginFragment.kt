package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.auth.localCreds
import calebxzhou.rdi.service.playerLogin
import calebxzhou.rdi.ui2.FragmentSize
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.component.RTextField
import calebxzhou.rdi.ui2.editText
import calebxzhou.rdi.util.ioScope
import icyllis.modernui.view.Gravity
import kotlinx.coroutines.launch

class LoginFragment : RFragment("登录") {
    private lateinit var qqInput: RTextField
    private lateinit var passwordInput: RTextField
    override var fragSize = FragmentSize.SMALL
    override fun initContent() {
        val creds = localCreds
        val loginInfos = creds.loginInfos
        contentLayout.apply {
            // Center children
            this.gravity = Gravity.CENTER_HORIZONTAL

            qqInput = editText("QQ号"){
            }
            passwordInput = editText("密码") {
                isPassword = true
            }
            val storedAccounts = loginInfos.entries.sortedByDescending { it.value.lastLoggedTime }
            if (storedAccounts.isNotEmpty()) {
                val accountLookup = storedAccounts.associate { it.key.toHexString() to it.value }
                qqInput.dropdownItems = storedAccounts.map { it.key.toHexString() }
                qqInput.onDropdownItemSelected = { selected ->
                    accountLookup[selected]?.let { info ->
                        passwordInput.setText(info.pwd)
                    }
                }
                qqInput.edit.setOnLongClickListener {
                    qqInput.openDropdown()
                    true
                }
                creds.lastLogged?.let { last ->
                    val lastId = last.qq
                    qqInput.setText(lastId)
                    passwordInput.setText(last.pwd)
                }
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