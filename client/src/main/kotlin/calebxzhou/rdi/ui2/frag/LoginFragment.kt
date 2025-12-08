package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.auth.localCreds
import calebxzhou.rdi.service.playerLogin
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.component.RTextField
import calebxzhou.rdi.ui2.component.alertErr
import calebxzhou.rdi.util.ioScope
import icyllis.modernui.view.Gravity
import kotlinx.coroutines.launch

class LoginFragment : RFragment("登录") {
    private lateinit var qqInput: RTextField
    private lateinit var passwordInput: RTextField
    override var fragSize = FragmentSize.SMALL
    init {
        contentViewInit = {
            val creds = localCreds
            val loginInfos = creds.loginInfos
            gravity = Gravity.CENTER_HORIZONTAL

            qqInput = textField("RDID/QQ号") {
                padding8dp()
            }
            passwordInput = textField("密码") {
                isPassword = true
            }
            val storedAccounts = loginInfos.entries.sortedByDescending { it.value.lastLoggedTime }
            if (storedAccounts.isNotEmpty()) {
                val accountLookup = storedAccounts.associate { it.key.toHexString() to it.value }
                qqInput.dropdownItems = storedAccounts.map { it.key.toHexString() }
                qqInput.onDropdownItemSelected = { selected ->
                    accountLookup[selected]?.let { info ->
                        passwordInput.text = info.pwd
                    }
                }
                qqInput.edit.setOnLongClickListener {
                    qqInput.openDropdown()
                    true
                }
                creds.lastLogged?.let { last ->
                    val lastId = last.qq
                    qqInput.text = lastId
                    passwordInput.text = last.pwd
                }
            }
            linearLayout {
                center()
                /*textView("[单人创造]"){
                    padding8dp()
                    setOnClickListener {
                        LevelService.openFlatLevel()
                    }
                }
                textView("[设置]"){
                    padding8dp()
                    setOnClickListener { mc set OptionsScreen(this@LoginFragment.mcScreen,mc.options) }
                }*/

            }
            bottomOptionsConfig = {
                "登录" colored MaterialColor.BLUE_800 with { onClicked() }
                "注册" colored MaterialColor.GREEN_900 with { RegisterFragment().go() }
            }
        }
    }

    private fun onClicked() {
        val qq = qqInput.edit.text.toString()
        val pwd = passwordInput.txt
        if(qq.isBlank() || pwd.isBlank()){
            alertErr("未填写完整")
        }
        ioScope.launch {
            playerLogin(qq, pwd)
        }

    }
}