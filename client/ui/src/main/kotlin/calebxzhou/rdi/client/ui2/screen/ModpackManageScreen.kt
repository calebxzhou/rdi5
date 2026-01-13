package calebxzhou.rdi.client.ui2.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
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
import androidx.compose.ui.unit.dp
import calebxzhou.mykotutils.std.deleteRecursivelyNoSymlink
import calebxzhou.rdi.client.net.server
import calebxzhou.rdi.client.service.ModpackService
import calebxzhou.rdi.client.service.ModpackService.startInstall
import calebxzhou.rdi.client.ui2.MaterialColor
import calebxzhou.rdi.client.ui2.alertErr
import calebxzhou.rdi.client.ui2.asIconText
import calebxzhou.rdi.common.model.Modpack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop

/**
 * calebxzhou @ 2026-01-13 23:14
 */
@Composable
fun ModpackManageScreen() {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var localDirs by remember { mutableStateOf<List<ModpackService.LocalDir>>(emptyList()) }
    var confirmDelete by remember { mutableStateOf<ModpackService.LocalDir?>(null) }

    fun reload() {
        loading = true
        errorMessage = null
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { ModpackService.getLocalPackDirs() }.getOrNull()
            }
            if (result == null) {
                errorMessage = "加载本地整合包失败"
            } else {
                localDirs = result
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) {
        reload()
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "本地整合包版本管理",
            style = MaterialTheme.typography.h6,
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

        errorMessage?.let { Text(it, color = MaterialTheme.colors.error) }

        if (!loading && localDirs.isEmpty()) {
            Text("尚未安装本地整合包", color = Color.Black)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(localDirs, key = { "${it.modpackId}_${it.verName}" }) { packdir ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${packdir.modpackName} ${packdir.verName}",
                        color = Color.Black,
                        style = MaterialTheme.typography.body1
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { confirmDelete = packdir },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialColor.RED_900.color,
                                contentColor = Color.White
                            )
                        ) {
                            Text("\uEA81 删除".asIconText)
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    val response = withContext(Dispatchers.IO) {
                                        runCatching { server.makeRequest<Modpack>("modpack/${packdir.modpackId}") }
                                            .getOrNull()
                                    }
                                    val modpack = response?.data
                                    val version = modpack?.versions?.find { it.name == packdir.verName }
                                    if (modpack == null || version == null) {
                                        errorMessage=("未找到对应版本信息，可能已被删除")
                                        return@launch
                                    }
                                    version.startInstall(modpack.mcVer, modpack.modloader, modpack.name)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = MaterialColor.PINK_900.color,
                                contentColor = Color.White
                            )
                        ) {
                            Text("\uDB81\uDC53 重装".asIconText)
                        }
                        Button(
                            onClick = {
                                val dir = packdir.dir
                                if (!dir.exists()) {
                                    errorMessage=("目录不存在: ${dir.absolutePath}")
                                    return@Button
                                }
                                runCatching {
                                    if (Desktop.isDesktopSupported()) {
                                        Desktop.getDesktop().open(dir)
                                    } else {
                                        ProcessBuilder("explorer", dir.absolutePath).start()
                                    }
                                }.onFailure {
                                    errorMessage=("无法打开目录: ${it.message}")
                                }
                            }
                        ) {
                            Text("\uEAED 打开文件夹".asIconText)
                        }
                    }
                }
            }
        }
    }

    confirmDelete?.let { packdir ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除本地整合包版本 ${packdir.modpackName} ${packdir.verName} 吗？") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = null
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            packdir.dir.deleteRecursivelyNoSymlink()
                        }
                        reload()
                    }
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
