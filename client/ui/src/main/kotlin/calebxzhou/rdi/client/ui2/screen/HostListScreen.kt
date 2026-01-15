package calebxzhou.rdi.client.ui2.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import calebxzhou.rdi.client.Const
import calebxzhou.rdi.client.auth.LocalCredentials
import calebxzhou.rdi.client.net.server
import calebxzhou.rdi.client.ui2.BackButton
import calebxzhou.rdi.client.ui2.CircleIconButton
import calebxzhou.rdi.client.ui2.MainColumn
import calebxzhou.rdi.client.ui2.MaterialColor
import calebxzhou.rdi.client.ui2.SimpleTooltip
import calebxzhou.rdi.client.ui2.TitleRow
import calebxzhou.rdi.client.ui2.comp.HostCard
import calebxzhou.rdi.common.model.Host
import org.bson.types.ObjectId

/**
 * calebxzhou @ 2026-01-15 14:15
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostListScreen(
    onBack: (() -> Unit),
    onOpenWorldList: (() -> Unit)? = null
) {
    var hosts by remember { mutableStateOf<List<Host.BriefVo>>(emptyList()) }
    var showMy by remember { mutableStateOf(true) }
    var showCarrierDialog by remember { mutableStateOf(false) }
    val creds = remember { LocalCredentials.read() }
    var selectedCarrier by remember { mutableStateOf(creds.carrier) }

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
            Checkbox(
                checked = showMy,
                onCheckedChange = { showMy = it }
            )
            Text(text = "我受邀的")

            Spacer(modifier = Modifier.width(16.dp))
            CircleIconButton("\uDB85\uDC5C","存档数据管理"){
                onOpenWorldList?.invoke()
            }
            Spacer(modifier = Modifier.width(8.dp))
            CircleIconButton("\uEF09","节点"){ showCarrierDialog = true }
            Spacer(modifier = Modifier.width(8.dp))

            CircleIconButton("\uF067","创建新地图"){

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
                    host.HostCard(onClickPlay = { })
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
    if (showCarrierDialog) {
        CarrierDialog(
            selected = selectedCarrier,
            onSelect = { carrier ->
                selectedCarrier = carrier
                creds.carrier = carrier
                creds.save()
            },
            onDismiss = { showCarrierDialog = false }
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
@Composable
fun CarrierDialog(
    selected: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val carriers = listOf("电信", "移动", "联通", "教育网", "广电")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择运营商节点") },
        text = {
            Column {
                carriers.forEachIndexed { index, name ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(index) }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selected == index,
                            onClick = { onSelect(index) }
                        )
                        Text(name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭")
            }
        }
    )
}
