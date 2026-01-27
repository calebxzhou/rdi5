package calebxzhou.rdi.client.ui2.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import calebxzhou.rdi.client.Const
import calebxzhou.rdi.client.net.loggedAccount
import calebxzhou.rdi.client.net.server
import calebxzhou.rdi.client.service.ModpackService.StartPlayResult
import calebxzhou.rdi.client.service.ModpackService.startPlay
import calebxzhou.rdi.client.ui2.*
import calebxzhou.rdi.client.ui2.comp.HeadButton
import calebxzhou.rdi.client.ui2.comp.HostCard
import calebxzhou.rdi.common.model.Host
import calebxzhou.rdi.common.model.Task
import kotlinx.coroutines.launch
import org.bson.types.ObjectId

/**
 * calebxzhou @ 2026-01-15 14:15
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostListScreen(
    onBack: (() -> Unit),
    onOpenWardrobe: (() -> Unit)? = null,
    onOpenMail: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onOpenWorldList: (() -> Unit)? = null,
    onOpenHostInfo: ((String) -> Unit)? = null,
    onOpenModpackList: (() -> Unit)? = null,
    onOpenMcPlay: ((McPlayArgs) -> Unit)? = null,
    onOpenTask: ((Task) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    var hosts by remember { mutableStateOf<List<Host.BriefVo>>(emptyList()) }
    var showMy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var installConfirmTask by remember { mutableStateOf<Task?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(showMy) {
        if (Const.USE_MOCK_DATA) {
            hosts = generateMockHosts()
        } else {
            val endpoint = if (showMy) "host/my" else "host/lobby/0"
            val response = server.makeRequest<List<Host.BriefVo>>(endpoint)
            hosts = response.data ?: emptyList()
        }
    }
    MainColumn{
        // Header / Toolbar
        TitleRow("地图大厅", onBack = onBack){
            errorMessage?.let { Text(it, color = MaterialTheme.colors.error) }
            Checkbox(
                checked = showMy,
                onCheckedChange = { showMy = it }
            )
            Text(text = "我的图")
            Space8w()
            HeadButton(loggedAccount._id){
                onOpenWardrobe?.invoke()
            }
            Space8w()
            ImageIconButton("grass_block","MC资源及整合包管理",
                bgColor = Color.LightGray) {
                onOpenModpackList?.invoke()
            }
            Space8w()
            CircleIconButton("\uDB85\uDC5C","存档数据管理"){
                onOpenWorldList?.invoke()
            }
            Space8w()
            CircleIconButton(
                "\uEB51",
                "设置",
            ) {
                onOpenSettings?.invoke()
            }
            Space8w()
            CircleIconButton("\uEB1C" ,"信箱") {
                onOpenMail?.invoke()
            }

        }
        Spacer(modifier = Modifier.height(16.dp))

        if (hosts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "暂无可展示的地图",
                    style = MaterialTheme.typography.h6,
                    color = Color.Gray
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 300.dp),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(hosts) { host ->
                    host.HostCard(onClickPlay = {
                        scope.launch {
                            val res = server.makeRequest<Host.DetailVo>("host/${host._id}/detail")
                            val detail = res.data
                                ?: run {
                                    errorMessage = "获取地图信息失败: ${res.msg}"
                                    return@launch
                                }
                            val args = try {
                                detail.startPlay()
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "无法开始游玩"
                                return@launch
                            }
                            when (args) {
                                is StartPlayResult.Ready -> {
                                    if (onOpenMcPlay != null) {
                                        onOpenMcPlay(args.args)
                                    } else {
                                        errorMessage = "暂不支持在此页面游玩"
                                    }
                                }
                                is StartPlayResult.NeedInstall -> {
                                    installConfirmTask = args.task
                                }
                            }
                        }
                    }, onClick = {
                        onOpenHostInfo?.invoke(host._id.toHexString())
                    })
                    /*calebxzhou.rdi.client.ui.component.HostCard(
                        host = host,
                        onClick = {
                            HostInfoFragment(host._id).go()
                        },
                        onPlayClick = {
                            scope.launch {
                                val res = server.makeRequest<Host>("host/${host._id}")
                                res.data?.startPlay()
                            }
                        }
                    )*/
                }
            }
        }
    }
    installConfirmTask?.let { task ->
        AlertDialog(
            onDismissRequest = { installConfirmTask = null },
            title = { Text("未下载整合包") },
            text = { Text("未下载此地图的整合包，是否立即下载？") },
            confirmButton = {
                TextButton(onClick = {
                    installConfirmTask = null
                    if (onOpenTask != null) {
                        onOpenTask(task)
                    } else {
                        errorMessage = "暂不支持在此页面下载"
                    }
                }) { Text("下载") }
            },
            dismissButton = {
                TextButton(onClick = { installConfirmTask = null }) { Text("取消") }
            }
        )
    }
}

private fun generateMockHosts(): List<Host.BriefVo> = List(50) { index ->
    val base = Host.BriefVo.TEST
    base.copy(
        _id = ObjectId(),
        name = "${base.name} #${index + 1}",
        port = base.port + index
    )
}

