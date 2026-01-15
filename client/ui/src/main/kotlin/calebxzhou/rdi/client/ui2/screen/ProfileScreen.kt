package calebxzhou.rdi.client.ui2.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberTooltipState
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import calebxzhou.mykotutils.std.jarResource
import calebxzhou.rdi.RDIClient
import calebxzhou.rdi.client.net.loggedAccount
import calebxzhou.rdi.client.net.server
import calebxzhou.rdi.client.service.PlayerInfoCache
import calebxzhou.rdi.client.service.PlayerService
import calebxzhou.rdi.client.ui2.CircleIconButton
import calebxzhou.rdi.client.ui2.CodeFontFamily
import calebxzhou.rdi.client.ui2.MaterialColor
import calebxzhou.rdi.client.ui2.SimpleTooltip
import calebxzhou.rdi.client.ui2.asIconText
import calebxzhou.rdi.client.ui2.comp.HeadButton
import calebxzhou.rdi.common.model.RAccount
import io.ktor.http.HttpMethod
import kotlinx.coroutines.launch

/**
 * calebxzhou @ 2026-01-14 19:49
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: (() -> Unit)? = null,
    onOpenWardrobe: (() -> Unit)? = null,
    onOpenHostList: (() -> Unit)? = null,
    onOpenMail: (() -> Unit)? = null,
    onOpenModpackManage: (() -> Unit)? = null,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showChangeDialog by remember { mutableStateOf(false) }
    val grassIcon = remember {
        RDIClient.jarResource("assets/icons/grass_block.png").use { it.readBytes().decodeToImageBitmap() }
    }
    val tooltipState = rememberTooltipState()
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                HeadButton(loggedAccount._id)
                SimpleTooltip("MC资源及整合包管理") {
                    TextButton(
                        onClick = { onOpenModpackManage?.invoke() },
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Transparent
                        )
                    ) {
                        Image(
                            bitmap = grassIcon,
                            contentDescription = "Grass Block",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(
                    "⏻",
                    "退出登录",
                    bgColor = MaterialColor.RED_900.color,
                    contentPadding = PaddingValues(start = 1.dp)
                ) {
                    loggedAccount = RAccount.DEFAULT
                    onLogout?.invoke()
                }
                Spacer(Modifier.width(12.dp))
                CircleIconButton(
                    "\uEB51",
                    "账户信息设置",
                    bgColor = Color.Gray
                ) {
                    showChangeDialog = true
                }
                Spacer(Modifier.width(12.dp))
                CircleIconButton("\uEE1C","衣柜", bgColor = MaterialColor.PINK_900.color) {
                    onOpenWardrobe?.invoke()
                }
                Spacer(Modifier.width(12.dp))
                CircleIconButton("\uEB1C" ,"信箱", bgColor = Color.Gray) {
                    onOpenMail?.invoke()
                }
                Spacer(Modifier.width(12.dp))
                CircleIconButton("\uF04B","地图大厅", bgColor = MaterialColor.GREEN_900.color, contentPadding = PaddingValues(start = 2.dp)) {
                    onOpenHostList?.invoke()
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.5f)
        )
    }

    if (showChangeDialog) {
        ChangeProfileDialog(
            onDismiss = { showChangeDialog = false },
            onSuccess = {
                showChangeDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "修改成功",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        )
    }

}

@Composable
private fun ChangeProfileDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val account = loggedAccount
    var name by remember { mutableStateOf(account.name) }
    var pwd by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = { Text("修改信息") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("昵称") },
                    singleLine = true,
                    enabled = !submitting,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pwd,
                    onValueChange = { pwd = it },
                    label = { Text("新密码 留空则不修改") },
                    singleLine = true,
                    enabled = !submitting,
                    visualTransformation = if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        Text(
                            text = "\uDB80\uDE08".asIconText,
                            style = MaterialTheme.typography.h6.copy(
                                fontFamily = CodeFontFamily,
                                fontSize = 20.sp
                            ),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clickable { showPassword = !showPassword }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let { Text(it, color = MaterialTheme.colors.error) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !submitting,
                onClick = {
                    val nameBytes = name.toByteArray(Charsets.UTF_8).size
                    if (nameBytes !in 3..24) {
                        errorMessage = "昵称须在3~24个字节，当前为${nameBytes}"
                        return@TextButton
                    }
                    if (pwd.isNotEmpty() && pwd.length !in 6..16) {
                        errorMessage = "密码长度须在6~16个字符"
                        return@TextButton
                    }
                    val params = mutableMapOf<String, Any>()
                    if (name != account.name) params["name"] = name
                    if (pwd.isNotEmpty() && pwd != account.pwd) params["pwd"] = pwd
                    if (params.isEmpty()) {
                        errorMessage = "没有修改内容"
                        return@TextButton
                    }
                    submitting = true
                    errorMessage = null
                    scope.launch {
                        runCatching {
                            server.makeRequest<Unit>("player/profile", HttpMethod.Put, params)
                            loggedAccount = PlayerService.login(account.qq, pwd).getOrThrow()
                            PlayerInfoCache -= loggedAccount._id
                        }.getOrElse {
                            errorMessage = "修改失败: ${it.message}"
                            return@launch
                        }
                        submitting = false
                        onSuccess()
                    }
                }
            ) {
                if (submitting) {
                    CircularProgressIndicator(modifier = Modifier.width(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("修改")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!submitting) onDismiss() }) {
                Text("取消")
            }
        }
    )
}
