package calebxzhou.rdi.client.ui.frag

import calebxzhou.mykotutils.std.jarResource
import calebxzhou.mykotutils.std.javaExePath
import calebxzhou.mykotutils.std.readAllString
import calebxzhou.rdi.client.auth.localCreds
import calebxzhou.rdi.client.service.playerLogin
import calebxzhou.rdi.client.ui.*
import calebxzhou.rdi.client.ui.component.*
import calebxzhou.rdi.common.util.ioScope
import icyllis.modernui.view.Gravity
import kotlinx.coroutines.launch
import java.io.File

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
                textView("[创建桌面快捷方式]"){
                    padding8dp()
                    setOnClickListener {
                        ioScope.launch {
                            runCatching {
                                val osName = System.getProperty("os.name")
                                if (!osName.contains("windows", ignoreCase = true)) {
                                    alertErr("仅支持 Windows")
                                    return@launch
                                }
                                val baseDir = File(".").absoluteFile
                                val javaExe = File(javaExePath)
                                val javawCandidate = javaExe.parentFile?.resolve("javaw.exe")
                                val javawPath = when {
                                    javaExe.name.equals("java.exe", ignoreCase = true) && javawCandidate?.exists() == true ->
                                        javawCandidate
                                    javaExe.name.equals("javaw.exe", ignoreCase = true) -> javaExe
                                    else -> javaExe
                                }
                                if (!javawPath.exists()) {
                                    alertErr("未找到javaw: ${javawPath.absolutePath}")
                                    return@launch
                                }
                                val resourcesDir = File(baseDir, "resources").apply { mkdirs() }
                                val iconFile = File(resourcesDir, "icon.ico")
                                if (!iconFile.exists()) {
                                    this@LoginFragment.jarResource("icon.ico").use { input ->
                                        iconFile.outputStream().use { output -> input.copyTo(output) }
                                    }
                                }
                                val args = "-cp \"lib/*;rdi-5-ui.jar\" calebxzhou.rdi.RDIKt"

                                fun esc(path: String) = path.replace("'", "''")
                                val template = this@LoginFragment.jarResource("shortcut_maker.ps1").readAllString()
                                val script = template
                                    .replace("__JAVAW__", esc(javawPath.absolutePath))
                                    .replace("__ARGS__", esc(args))
                                    .replace("__WORKDIR__", esc(baseDir.absolutePath))
                                    .replace("__ICON__", esc(iconFile.absolutePath))
                                val scriptFile = File(resourcesDir, "shortcut_maker.ps1")
                                scriptFile.writeText(script)

                                val psCommands = listOf("powershell", "pwsh")
                                val proc = psCommands.firstNotNullOfOrNull { cmd ->
                                    runCatching {
                                        ProcessBuilder(
                                            cmd,
                                            "-NoProfile",
                                            "-ExecutionPolicy",
                                            "Bypass",
                                            "-File",
                                            scriptFile.absolutePath
                                        ).redirectErrorStream(true).start()
                                    }.getOrNull()
                                }
                                if (proc == null) {
                                    alertErr("未找到 PowerShell 或 pwsh")
                                    return@launch
                                }
                                val exit = proc.waitFor()
                                val shortcutPath = File(System.getProperty("user.home"), "Desktop")
                                    .resolve("RDI.lnk")
                                if (exit != 0 || !shortcutPath.exists()) {
                                    alertErr("创建快捷方式失败")
                                    return@launch
                                }
                                alertOk("已创建桌面快捷方式：RDI")
                            }.onFailure {
                                alertErr("创建快捷方式失败: ${it.message}")
                            }
                        }
                    }
                }

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
            showLoading()
            playerLogin(qq, pwd).getOrElse { alertErr(it.message?:"未知原因") }
            closeLoading()
        }

    }
}
