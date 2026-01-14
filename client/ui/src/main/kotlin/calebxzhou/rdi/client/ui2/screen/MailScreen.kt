package calebxzhou.rdi.client.ui2.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import calebxzhou.mykotutils.std.secondsToHumanDateTime
import calebxzhou.rdi.client.net.server
import calebxzhou.rdi.client.ui2.MaterialColor
import calebxzhou.rdi.common.json
import calebxzhou.rdi.common.model.Mail
import calebxzhou.rdi.common.model.Response
import calebxzhou.rdi.common.net.json
import io.ktor.client.call.body
import io.ktor.client.request.setBody
import io.ktor.http.HttpMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bson.types.ObjectId

/**
 * calebxzhou @ 2026-01-13 23:19
 */
@Composable
fun MailScreen() {
    val scope = rememberCoroutineScope()
    var mails by remember { mutableStateOf<List<Mail.Vo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedIds by remember { mutableStateOf<Set<ObjectId>>(emptySet()) }
    var confirmDelete by remember { mutableStateOf(false) }
    var detailId by remember { mutableStateOf<ObjectId?>(null) }

    fun reload() {
        loading = true
        errorMessage = null
        scope.launch {
            val response = withContext(Dispatchers.IO) {
                runCatching { server.makeRequest<List<Mail.Vo>>("mail") }.getOrNull()
            }
            loading = false
            if (response == null) {
                errorMessage = "加载信箱失败"
                mails = emptyList()
                selectedIds = emptySet()
                return@launch
            }
            if (!response.ok) {
                errorMessage = response.msg
                mails = emptyList()
                selectedIds = emptySet()
                return@launch
            }
            mails = response.data ?: emptyList()
            val visible = mails.mapTo(mutableSetOf()) { it.id }
            selectedIds = selectedIds.filter { it in visible }.toSet()
        }
    }

    LaunchedEffect(Unit) {
        reload()
    }

    val allSelected = mails.isNotEmpty() && selectedIds.size == mails.size

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("信箱", style = MaterialTheme.typography.h6, color = Color.Black)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = allSelected,
                        onCheckedChange = { checked ->
                            selectedIds = if (checked) {
                                mails.map { it.id }.toSet()
                            } else {
                                emptySet()
                            }
                        }
                    )
                    Text("全选")
                }
                Button(
                    onClick = {
                        if (selectedIds.isEmpty()) {
                            errorMessage = "请选择至少一封邮件"
                        } else {
                            confirmDelete = true
                        }
                    },
                    colors = androidx.compose.material.ButtonDefaults.buttonColors(
                        backgroundColor = MaterialColor.RED_900.color,
                        contentColor = Color.White
                    )
                ) {
                    Text("删除所选")
                }
            }
        }

        if (loading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        }

        errorMessage?.let { Text(it, color = MaterialTheme.colors.error) }

        if (!loading && mails.isEmpty()) {
            Text("什么都没有~", color = Color.Black)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(mails, key = { it.id.toHexString() }) { mail ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { detailId = mail.id }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedIds.contains(mail.id),
                            onCheckedChange = { checked ->
                                selectedIds = if (checked) {
                                    selectedIds + mail.id
                                } else {
                                    selectedIds - mail.id
                                }
                            }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mail.title,
                                style = MaterialTheme.typography.subtitle1,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${mail.intro}...",
                                style = MaterialTheme.typography.body2,
                                fontStyle = FontStyle.Italic,
                                color = MaterialColor.GRAY_500.color,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text("来自 ${mail.senderName}", style = MaterialTheme.typography.caption)
                        Text(
                            text = mail.id.timestamp.secondsToHumanDateTime,
                            style = MaterialTheme.typography.caption
                        )
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("确认删除") },
            text = { Text("要删除所选的邮件吗？") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        val payload = selectedIds.toList().json
                        val response = withContext(Dispatchers.IO) {
                            runCatching {
                                server.createRequest("mail", HttpMethod.Delete) {
                                    json()
                                    setBody(payload)
                                }.body<Response<Unit>>()
                            }.getOrNull()
                        }
                        if (response == null) {
                            errorMessage = "删除失败"
                            return@launch
                        }
                        if (!response.ok) {
                            errorMessage = response.msg
                            return@launch
                        }
                        selectedIds = emptySet()
                        reload()
                    }
                }) {
                    Text("删除", color = MaterialTheme.colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("取消")
                }
            }
        )
    }

    detailId?.let { id ->
        MailDetailDialog(
            id = id,
            onDismiss = { detailId = null },
            onDelete = {
                scope.launch {
                    val response = withContext(Dispatchers.IO) {
                        runCatching {
                            server.makeRequest<Unit>("mail/${id}", HttpMethod.Delete)
                        }.getOrNull()
                    }
                    if (response == null) {
                        errorMessage = "删除失败"
                        return@launch
                    }
                    if (!response.ok) {
                        errorMessage = response.msg
                        return@launch
                    }
                    detailId = null
                    reload()
                }
            }
        )
    }
}

@Composable
private fun MailDetailDialog(
    id: ObjectId,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var mail by remember { mutableStateOf<Mail?>(null) }

    LaunchedEffect(id) {
        loading = true
        errorMessage = null
        val response = withContext(Dispatchers.IO) {
            runCatching { server.makeRequest<Mail>("mail/${id}") }.getOrNull()
        }
        loading = false
        if (response == null) {
            errorMessage = "加载邮件失败"
        } else if (!response.ok) {
            errorMessage = response.msg
        } else {
            mail = response.data
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(mail?.title ?: "详细内容") },
        text = {
            when {
                loading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.width(24.dp))
                    }
                }
                errorMessage != null -> {
                    Text(errorMessage ?: "加载失败", color = MaterialTheme.colors.error)
                }
                mail != null -> {
                    Text(mail?.content.orEmpty())
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text("删除", color = MaterialTheme.colors.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
