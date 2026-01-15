package calebxzhou.rdi.client.ui2.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import calebxzhou.rdi.client.net.loggedAccount
import calebxzhou.rdi.client.net.server
import calebxzhou.rdi.client.ui2.MaterialColor
import calebxzhou.rdi.client.ui2.asIconText
import calebxzhou.rdi.client.ui2.comp.HeadButton
import calebxzhou.rdi.common.model.RAccount
import io.ktor.http.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * calebxzhou @ 2026-01-14 19:49
 */
@Composable
fun ProfileScreen(
    onLogout: (() -> Unit)? = null
){
    var showChangeDialog by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = {
                    loggedAccount = RAccount.DEFAULT
                    onLogout?.invoke()
                },
                modifier = Modifier.width(28.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = MaterialColor.RED_900.color
                )
            ) {
                Text("⏻".asIconText)
            }
            HeadButton(loggedAccount._id)
            //settings
            TextButton(
                modifier = Modifier.width(28.dp),
                onClick = {
                    showChangeDialog = true
                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = MaterialColor.BLACK.color
                )
            ){
                Text("\uEB51".asIconText)
            }
            //cloth
            TextButton(
                modifier = Modifier.width(28.dp),
                onClick = {

                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = MaterialColor.TEAL_900.color
                )
            ){
                Text("\uEE1C".asIconText)
            }
            //mail
            TextButton(
                modifier = Modifier.width(28.dp),
                onClick = {

                },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = MaterialColor.BLUE_900.color
                )
            ){
                Text("\uEB1C".asIconText)
            }
        }
    }

    if (showChangeDialog) {
        ChangeProfileDialog(
            onDismiss = { showChangeDialog = false }
        )
    }
}

@Composable
private fun ChangeProfileDialog(
    onDismiss: () -> Unit
) {
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
                        val response = withContext(Dispatchers.IO) {
                            runCatching {
                                server.makeRequest<Unit>("player/profile", HttpMethod.Put, params)
                            }.getOrNull()
                        }
                        submitting = false
                        if (response == null) {
                            errorMessage = "修改失败"
                            return@launch
                        }
                        if (!response.ok) {
                            errorMessage = response.msg
                            return@launch
                        }
                        val finalPwd = if (pwd.isEmpty()) account.pwd else pwd
                        loggedAccount = RAccount(
                            account._id,
                            name,
                            finalPwd,
                            account.qq,
                            account.score,
                            account.cloth
                        )
                        onDismiss()
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
