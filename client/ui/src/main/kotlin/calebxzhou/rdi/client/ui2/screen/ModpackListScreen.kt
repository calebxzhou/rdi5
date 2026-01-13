package calebxzhou.rdi.client.ui2.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import calebxzhou.rdi.client.net.loggedAccount
import calebxzhou.rdi.client.net.server
import calebxzhou.rdi.client.ui2.asIconText
import calebxzhou.rdi.client.ui2.comp.ModpackCard
import calebxzhou.rdi.common.model.ModpackVo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * calebxzhou @ 2026-01-13 18:27
 */
@Composable
fun ModpackListScreen(
) {
    var modpacks by remember { mutableStateOf<List<ModpackVo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var onlyMine by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loading = true
        errorMessage = null
        val response = withContext(Dispatchers.IO) {
            runCatching { server.makeRequest<List<ModpackVo>>("modpack") }.getOrNull()
        }
        if (response == null) {
            errorMessage = "加载整合包失败"
        } else if (!response.ok) {
            errorMessage = response.msg
        } else {
            modpacks = response.data ?: emptyList()
        }
        loading = false
    }

    val visibleModpacks = if (onlyMine) {
        modpacks.filter { it.authorId == loggedAccount._id }
    } else {
        modpacks
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "整合包列表",
                style = MaterialTheme.typography.h6,
                color = Color.Black
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = onlyMine,
                        onCheckedChange = { onlyMine = it }
                    )
                    Text("只看我的包")
                }
                Button(onClick = { TODO() }) {
                    Text("\uF0C7 已安装的包".asIconText)
                }
                Button(
                    onClick = { TODO() }
                ) {
                    Text("\uDB80\uDFD5 上传新包".asIconText)
                }
            }
        }

        Text(
            text = "选择想玩的整合包及版本创建地图。如果没有想玩的，可以上传自己的整合包。",
            color = Color.Black
        )

        if (loading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        }

        errorMessage?.let {
            Text(it, color = MaterialTheme.colors.error)
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(280.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(visibleModpacks, key = { it.id.toHexString() }) { modpack ->
                modpack.ModpackCard(onClick = { TODO() })
            }
        }
    }
}
