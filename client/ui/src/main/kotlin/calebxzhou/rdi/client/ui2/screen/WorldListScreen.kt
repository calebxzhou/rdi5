package calebxzhou.rdi.client.ui2.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import calebxzhou.mykotutils.std.secondsToHumanDateTime
import calebxzhou.rdi.client.net.rdiRequest
import calebxzhou.rdi.client.net.rdiRequestU
import calebxzhou.rdi.client.ui2.*
import calebxzhou.rdi.common.model.World
import io.ktor.http.*

/**
 * calebxzhou @ 2026-01-15 21:16
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldListScreen(
    onBack: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    var worlds by remember { mutableStateOf<List<World>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf<World?>(null) }
    var confirmCopy by remember { mutableStateOf<World?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var okMessage by remember { mutableStateOf<String?>(null) }
    fun reload() {
        loading = true
        errorMessage = null
        scope.rdiRequest<List<World>>(
            "world",
            onDone = {
                loading=false
            },
            onErr = {
                errorMessage = "加载区块数据失败:${it.message}"
                worlds = emptyList()
            },
            onOk = {
                worlds = it.data ?: emptyList()
            }
        )
    }
    LaunchedEffect(Unit) {
        reload()
    }
    LaunchedEffect(okMessage) {
        okMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            okMessage = null
        }
    }
    MainBox {
        MainColumn {
            TitleRow("区块管理", onBack = { onBack?.invoke() ?: Unit }) {
                errorMessage?.let { Text(it, color = MaterialTheme.colors.error) }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (loading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            }



            if (!loading && worlds.isEmpty()) {
                Text("没有区块数据。", color = Color.Gray)
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(worlds, key = { it._id.toHexString() }) { world ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "\uD83D\uDCBE ${world.name}",
                            style = MaterialTheme.typography.subtitle1
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "\uDB81\uDF52 \uE383  ${world._id.timestamp.secondsToHumanDateTime}".asIconText
                        )
                        Spacer(8.wM)
                        CircleIconButton(
                            icon = "\uF0C5",
                            tooltip = "复制",
                            bgColor = MaterialColor.GRAY_200.color,
                            iconColor = MaterialColor.GRAY_900.color
                        ) { confirmCopy = world }
                        Spacer(modifier = Modifier.width(8.dp))
                        CircleIconButton(
                            icon = "\uEA81",
                            tooltip = "删除",
                            bgColor = MaterialColor.RED_900.color
                        ) { confirmDelete = world }
                    }
                }
            }
        }
        BottomSnakebar(snackbarHostState)

    }
    confirmCopy?.let { world ->
        AlertDialog(
            onDismissRequest = { confirmCopy = null },
            title = { Text("确认复制") },
            text = { Text("要给区块数据“${world.name}”复制一份一模一样的吗？") },
            confirmButton = {
                TextButton(onClick = {
                    confirmCopy = null
                    scope.rdiRequestU(
                        "world/${world._id}/copy",
                        method = HttpMethod.Post,
                        onOk = {
                            okMessage = "已复制"
                            reload()
                        },
                        onErr = {
                            errorMessage = "复制失败: ${it.message}"
                        }
                    )
                }) {
                    Text("复制")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmCopy = null }) {
                    Text("取消")
                }
            }
        )
    }

    confirmDelete?.let { world ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("确认删除") },
            text = { Text("要永久删除区块数据“${world.name}”及其所有的回档点吗？无法恢复！") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = null
                    scope.rdiRequestU(
                        "world/${world._id}",
                        method = HttpMethod.Delete,
                        onOk = {
                            okMessage = "已删除"
                            reload()
                        },
                        onErr = {
                            errorMessage = "删除失败: ${it.message}"
                        }
                    )
                }) {
                    Text("删除", color = MaterialTheme.colors.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}
