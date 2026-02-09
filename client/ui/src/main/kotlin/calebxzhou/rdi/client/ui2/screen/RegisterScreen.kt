package calebxzhou.rdi.client.ui2.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import calebxzhou.rdi.client.net.rdiRequestU
import calebxzhou.rdi.client.net.server
import calebxzhou.rdi.client.service.PlayerService
import calebxzhou.rdi.client.ui2.BottomSnakebar
import calebxzhou.rdi.client.ui2.CircleIconButton
import calebxzhou.rdi.client.ui2.MainBox
import calebxzhou.rdi.client.ui2.MaterialColor
import calebxzhou.rdi.client.ui2.TitleRow
import calebxzhou.rdi.client.ui2.comp.PasswordField
import calebxzhou.rdi.common.exception.RequestError
import calebxzhou.rdi.common.json
import calebxzhou.rdi.common.model.MsaAccountInfo
import calebxzhou.rdi.common.model.RAccount
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.raphimc.minecraftauth.java.JavaAuthManager
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode
import net.raphimc.minecraftauth.util.holder.listener.ChangeListener
import sun.swing.SwingUtilities2.submit
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI

/**
 * calebxzhou @ 2026-02-08 16:59
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onBack: (() -> Unit)? = null,
    onRegisterSuccess: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var name by remember { mutableStateOf("") }
    var qq by remember { mutableStateOf("") }
    var pwd by remember { mutableStateOf("") }
    var pwd2 by remember { mutableStateOf("") }
    var msaDeviceCode by remember { mutableStateOf<MsaDeviceCode?>(null) }
    var showPassword by remember { mutableStateOf(false) }
    var showPassword2 by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var okMessage by remember { mutableStateOf<String?>(null) }
    var msaInfo by remember { mutableStateOf<MsaAccountInfo?>(null) }
    var mcName by remember { mutableStateOf<String?>(null) }
    var authManager by remember { mutableStateOf<JavaAuthManager?>(null) }
    LaunchedEffect(okMessage) {
        okMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            okMessage = null
        }
    }
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val manager = PlayerService.microsoftLogin { code ->
                msaDeviceCode = code
                // Open verification URI in user's browser
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(URI(code.directVerificationUri))
                }
            }
            // Login completed, get profile info directly
            authManager = manager
            msaInfo = MsaAccountInfo(
                manager.minecraftProfile.upToDate.id,
                manager.minecraftProfile.upToDate.name,
                manager.minecraftToken.upToDate.token,
            ).also { name = it.name }

        }
    }

    MainBox {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TitleRow(
                title = "注册",
                onBack = { onBack?.invoke() }
            ) {
            }
            Column(
                modifier = Modifier.fillMaxWidth(0.4f).align(Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("即将登录微软账号，点击复制浏览器中打开链接，请在5分钟内登录")
                Text("登录完成后稍等10秒，会自动读取账号信息以进行下一步")
                msaDeviceCode?.let { msaDeviceCode ->
                    Text(
                        text = msaDeviceCode.directVerificationUri,
                        color = MaterialTheme.colors.primary,
                        style = LocalTextStyle.current.copy(textDecoration = TextDecoration.Underline),
                        modifier = Modifier
                            .clickable {
                                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                                clipboard.setContents(StringSelection(msaDeviceCode.directVerificationUri), null)
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "链接已复制到剪贴板",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                            .pointerHoverIcon(PointerIcon.Hand)
                    )
                }

                msaInfo?.let { msaInfo ->
                    Text("登录成功！${msaInfo.name} · MSID ${msaInfo.uuid}")

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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                if (pwd != pwd2) {
                                    errorMessage = "两次输入的密码不一致"
                                    return@Button
                                }
                                if (name.isBlank() || qq.isBlank() || pwd.isBlank()) {
                                    errorMessage = "未填写完整"
                                    return@Button
                                }
                                submitting = true
                                errorMessage = null
                                scope.rdiRequestU(
                                    "player/register", body = RAccount.RegisterDto(name, qq, pwd, msaInfo).json,
                                    onDone = { submitting = false },
                                    onErr = { errorMessage = it.message ?: "注册失败" }) {
                                    okMessage = "注册成功，请登录"
                                    onRegisterSuccess?.invoke()
                                }

                            },
                            enabled = !submitting
                        ) {
                            Text(if (submitting) "注册中..." else "注册")
                        }
                    }
                }
                errorMessage?.let { Text(it, color = MaterialTheme.colors.error) }
            }
        }
        BottomSnakebar(snackbarHostState)
    }
}
