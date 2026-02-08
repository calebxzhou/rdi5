package calebxzhou.rdi.client.ui2.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import calebxzhou.mykotutils.std.canCreateSymlink
import calebxzhou.mykotutils.std.jarResource
import calebxzhou.mykotutils.std.javaExePath
import calebxzhou.mykotutils.std.readAllString
import calebxzhou.rdi.RDIClient
import calebxzhou.rdi.client.auth.LocalCredentials
import calebxzhou.rdi.client.net.server
import calebxzhou.rdi.client.service.PlayerService
import calebxzhou.rdi.client.service.UpdateService
import calebxzhou.rdi.client.ui2.BottomSnakebar
import calebxzhou.rdi.client.ui2.MainBox
import calebxzhou.rdi.client.ui2.comp.PasswordField
import calebxzhou.rdi.common.exception.RequestError
import calebxzhou.rdi.lgr
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.exitProcess


/**
 * calebxzhou @ 2026-01-14 16:45
 */
@Composable
fun LoginScreen(
    onLoginSuccess: (() -> Unit)? = null
) {
    var showPassword by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val creds = remember { LocalCredentials.read() }
    val storedAccounts = remember(creds.loginInfos) {
        creds.loginInfos.entries.sortedByDescending { it.value.lastLoggedTime }
    }
    var qq by remember { mutableStateOf("") }
    var pwd by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var showAccounts by remember { mutableStateOf(false) }
    var showRegister by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var updateStatus by remember { mutableStateOf("正在检查更新...") }
    var updateDetail by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    var okMessage by remember { mutableStateOf<String?>(null) }
    var symlinkError by remember { mutableStateOf<String?>(null) }
    var updateCheckComplete by remember { mutableStateOf(false) }

    fun attemptLogin() {
        if (qq.isBlank() || pwd.isBlank()) {
            loginError = "未填写完整"
            return
        }
        if (submitting) return
        submitting = true
        loginError = null
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                PlayerService.login(qq, pwd)
            }
            submitting = false
            result.onFailure {
                loginError = it.message ?: "登录失败"
            }.onSuccess {
                onLoginSuccess?.invoke()
            }
        }
    }

    LaunchedEffect(storedAccounts) {
        if (qq.isBlank() && pwd.isBlank()) {
            creds.lastLogged?.let {
                qq = it.qq
                pwd = it.pwd
            }
        }
    }
    LaunchedEffect(Unit) {
        if (!canCreateSymlink()) {
            symlinkError = """RDI需要权限为Mod及资源文件创建软连接。
请点击上方【创建桌面快捷方式】按钮，从桌面图标运行RDI。"""
        }
        if (server.noUpdate) {
            updateStatus = "已跳过更新"
            updateDetail = ""
            updateCheckComplete = true
        } else {
            UpdateService.startUpdateFlow(
                onStatus = { updateStatus = it; lgr.info { it } },
                onDetail = { updateDetail = it; lgr.info { it } },
                onRestart = {
                    updateStatus = "更新完成，客户端将在 5 秒关闭..."
                    for (i in 5 downTo 1) {
                        updateDetail = "${i}秒"
                        kotlinx.coroutines.delay(1000)
                    }
                    exitProcess(0)
                }
            )
            updateCheckComplete = true
        }
    }
    LaunchedEffect(okMessage) {
        okMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            okMessage = null
        }
    }

    MainBox {
        Column(
            modifier = Modifier.fillMaxSize().background(Color.White).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("登录", style = MaterialTheme.typography.h5, color = Color.Black)

            Column(
                modifier = Modifier.fillMaxWidth(0.4f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = qq,
                        onValueChange = { qq = it },
                        label = { Text("RDID/QQ号") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().onKeyEvent { event ->
                            if (event.type == KeyEventType.KeyUp && event.key == Key.Enter) {
                                attemptLogin()
                                true
                            } else {
                                false
                            }
                        },
                        trailingIcon = {
                            TextButton(onClick = { showAccounts = true }) {
                                Text("▼")
                            }
                        },
                    )
                    DropdownMenu(
                        expanded = showAccounts,
                        onDismissRequest = { showAccounts = false }
                    ) {
                        storedAccounts.forEach { entry ->
                            val info = entry.value
                            DropdownMenuItem(onClick = {
                                qq = info.qq
                                pwd = info.pwd
                                showAccounts = false
                            }) {
                                Text("${info.name} (${info.qq})")
                            }
                        }
                        if (storedAccounts.isEmpty()) {
                            DropdownMenuItem(onClick = { showAccounts = false }) {
                                Text("暂无历史账号")
                            }
                        }
                    }
                }
                PasswordField(
                    value = pwd,
                    onValueChange = { pwd = it },
                    showPassword = showPassword,
                    onToggleVisibility = { showPassword = !showPassword },
                    onEnter = { attemptLogin() }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                createShortcut()
                            }
                            if (result.isSuccess) {
                                okMessage = "已创建桌面快捷方式"
                            } else {
                                loginError = result.exceptionOrNull()?.message ?: "创建快捷方式失败"
                            }
                        }
                    }) {
                        Text("创建桌面快捷方式", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { showRegister = true }) {
                        Text("注册")
                    }
                }
                symlinkError?.let { message ->
                    Text(message, color = MaterialTheme.colors.error)
                    Text(
                        "或者按下Win+R运行secpol.msc\n进入菜单“本地策略/用户权限/创建符号链接”，添加当前系统用户${System.getProperty("user.name")}，确定后重启电脑。",
                        color = Color(0xFFCC4C4C),
                        fontStyle = FontStyle.Italic
                    )
                }

                loginError?.let { message ->
                    Text(message, color = MaterialTheme.colors.error)
                }

                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.width(20.dp).height(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (submitting) {
                            CircularProgressIndicator(modifier = Modifier.width(16.dp), strokeWidth = 2.dp)
                        }
                    }
                    Button(
                        onClick = {
                            attemptLogin()
                        },
                        enabled = !submitting && updateCheckComplete
                    ) {
                        Text(if (submitting) "登录中..." else "登录")
                    }
                }

            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = updateStatus,
                    style = MaterialTheme.typography.caption,
                    color = MaterialTheme.colors.onSurface
                )
                if (updateDetail.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = updateDetail,
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface
                    )
                }
            }
        }
        BottomSnakebar(snackbarHostState)
    }

    if (showRegister) {
        RegisterDialog(
            onDismiss = {
                showRegister = false
            },
            onSuccess = {
                showRegister = false
                okMessage = "注册成功，请登录"
            }
        )
    }
}

@Composable
private fun RegisterDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var qq by remember { mutableStateOf("") }
    var pwd by remember { mutableStateOf("") }
    var pwd2 by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var showPassword2 by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("注册新账号") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("")
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("昵称 支持中文") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = qq,
                    onValueChange = { qq = it },
                    label = { Text("QQ号") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                PasswordField(
                    value = pwd,
                    onValueChange = { pwd = it },
                    label = "密码",
                    showPassword = showPassword,
                    onToggleVisibility = { showPassword = !showPassword },
                    onEnter = {}
                )
                PasswordField(
                    value = pwd2,
                    onValueChange = { pwd2 = it },
                    label = "确认密码",
                    showPassword = showPassword2,
                    onToggleVisibility = { showPassword2 = !showPassword2 },
                    onEnter = {}
                )
                errorMessage?.let { Text(it, color = MaterialTheme.colors.error) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !submitting,
                onClick = {
                    if (pwd != pwd2) {
                        errorMessage = "两次输入的密码不一致"
                        return@TextButton
                    }
                    if (name.isBlank() || qq.isBlank() || pwd.isBlank()) {
                        errorMessage = "未填写完整"
                        return@TextButton
                    }
                    submitting = true
                    scope.launch {
                        val err = withContext(Dispatchers.IO) {
                            runCatching {
                                val resp = server.makeRequest<Unit>(
                                    "player/register",
                                    HttpMethod.Post,
                                    params = mapOf("name" to name, "qq" to qq, "pwd" to pwd)
                                )
                                if (!resp.ok) throw RequestError(resp.msg)
                            }.exceptionOrNull()
                        }
                        submitting = false
                        if (err != null) {
                            errorMessage = err.message ?: "注册失败"
                        } else {
                            onSuccess()
                        }
                    }
                }
            ) {
                Text(if (submitting) "注册中..." else "注册")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

private fun createShortcut(): Result<Unit> {
    return runCatching {
        val osName = System.getProperty("os.name")
        if (!osName.contains("windows", ignoreCase = true)) {
            throw IllegalStateException("仅支持 Windows")
        }
        val baseDir = File(".").absoluteFile
        val javaExe = File(javaExePath)
        val javawCandidate = javaExe.parentFile?.resolve("javaw.exe")
        val javawPath = when {
            javaExe.name.equals("java.exe", ignoreCase = true) && javawCandidate?.exists() == true -> javawCandidate
            javaExe.name.equals("javaw.exe", ignoreCase = true) -> javaExe
            else -> javaExe
        }
        if (!javawPath.exists()) {
            throw IllegalStateException("未找到javaw: ${javawPath.absolutePath}")
        }
        val resourcesDir = File(baseDir, "resources").apply { mkdirs() }
        val iconFile = File(resourcesDir, "icon.ico")
        if (!iconFile.exists()) {
            RDIClient.jarResource("icon.ico").use { input ->
                iconFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
        val args = "-cp \"lib/*;rdi-5-ui.jar\" calebxzhou.rdi.client.MainKt"
        fun esc(path: String) = path.replace("'", "''")
        val template = RDIClient.jarResource("shortcut_maker.ps1").readAllString()
        val script = template
            .replace("__JAVAW__", esc(javawPath.absolutePath))
            .replace("__ARGS__", esc(args))
            .replace("__WORKDIR__", esc(baseDir.absolutePath))
            .replace("__ICON__", esc(iconFile.absolutePath))
        val scriptFile = File(resourcesDir, "shortcut_maker.ps1")
        // PowerShell on Windows expects UTF-16LE (or UTF-8 BOM). Use UTF-16LE to avoid garbled paths.
        scriptFile.outputStream().use { output ->
            output.write(byteArrayOf(0xFF.toByte(), 0xFE.toByte()))
            output.write(script.toByteArray(Charsets.UTF_16LE))
        }

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
            throw IllegalStateException("未找到 PowerShell 或 pwsh")
        }
        val exit = proc.waitFor()
        val shortcutPath = File(System.getProperty("user.home"), "Desktop").resolve("RDI.lnk")
        if (exit != 0 || !shortcutPath.exists()) {
            throw IllegalStateException("创建快捷方式失败")
        }
    }
}
