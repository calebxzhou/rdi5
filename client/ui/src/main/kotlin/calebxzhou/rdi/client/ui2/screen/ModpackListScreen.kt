package calebxzhou.rdi.client.ui2.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import calebxzhou.rdi.client.net.loggedAccount
import calebxzhou.rdi.client.net.rdiRequest
import calebxzhou.rdi.client.ui2.CircleIconButton
import calebxzhou.rdi.client.ui2.MainColumn
import calebxzhou.rdi.client.ui2.Space8h
import calebxzhou.rdi.client.ui2.Space8w
import calebxzhou.rdi.client.ui2.TitleRow
import calebxzhou.rdi.client.ui2.comp.ModpackCard
import calebxzhou.rdi.common.model.Modpack

/**
 * calebxzhou @ 2026-01-13 18:27
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModpackListScreen(
    onBack: (() -> Unit) = {},
    onOpenUpload: (() -> Unit) = {},
    onOpenInfo: ((String) -> Unit) = {}
) {
    var modpacks by remember { mutableStateOf<List<Modpack.BriefVo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var onlyMine by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loading = true
        errorMessage = null
        rdiRequest<List<Modpack.BriefVo>>(
            path = "modpack",
            onOk = { response -> modpacks = response.data ?: emptyList() },
            onErr = { errorMessage = "加载整合包失败: ${it.message}" },
            onDone = { loading = false }
        )
    }

    val visibleModpacks = if (onlyMine) {
        modpacks.filter { it.authorId == loggedAccount._id }
    } else {
        modpacks
    }

    MainColumn {
        TitleRow("大家的整合包", onBack) {
            Checkbox(
                checked = onlyMine,
                onCheckedChange = { onlyMine = it }
            )
            Text("只看我的包")
            Space8w()
            CircleIconButton("\uDB80\uDFD5", "上传新包") {
                onOpenUpload.invoke()
            }
        }
        Space8h()

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
                modpack.ModpackCard(onClick = { onOpenInfo(modpack.id.toHexString()) })
            }
        }
    }
}
