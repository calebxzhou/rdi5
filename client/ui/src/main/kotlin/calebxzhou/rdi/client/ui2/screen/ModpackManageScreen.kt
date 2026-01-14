package calebxzhou.rdi.client.ui2.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import calebxzhou.mykotutils.std.deleteRecursivelyNoSymlink
import calebxzhou.rdi.client.net.server
import calebxzhou.rdi.client.service.ModpackService
import calebxzhou.rdi.client.service.ModpackService.startInstall
import calebxzhou.rdi.client.ui2.DEFAULT_MODPACK_ICON
import calebxzhou.rdi.client.ui2.MaterialColor
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
            items(localDirs, key = { "${it.vo.id}_${it.verName}" }) { packdir ->
                ModpackManageCard(
                    packdir = packdir,
                    onDelete = { confirmDelete = packdir },
                    onReinstall = {
                        scope.launch {
                            val version = server.makeRequest<Modpack.Version>("modpack/${packdir.vo.id}/version/${packdir.verName}").data
                            if (version == null) {
                                errorMessage = "未找到对应版本信息，可能已被删除"
                                return@launch
                            }
                            version.startInstall(packdir.vo.mcVer,packdir.vo.modloader, packdir.vo.name)
                        }
                    },
                    onOpenFolder = {
                        val dir = packdir.dir
                        if (!dir.exists()) {
                            errorMessage = "目录不存在: ${dir.absolutePath}"
                        } else {
                            runCatching {
                                if (Desktop.isDesktopSupported()) {
                                    Desktop.getDesktop().open(dir)
                                } else {
                                    ProcessBuilder("explorer", dir.absolutePath).start()
                                }
                            }.onFailure {
                                errorMessage = "无法打开目录: ${it.message}"
                            }
                        }
                    }
                )
            }
        }
    }

    confirmDelete?.let { packdir ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("确认删除") },
            text = { Text("要删除整合包 ${packdir.vo.name} ${packdir.verName} 吗？") },
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

@Composable
fun ModpackManageCard(
    packdir: ModpackService.LocalDir,
    onDelete: () -> Unit,
    onReinstall: () -> Unit,
    onOpenFolder: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFF9F9FB),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        elevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    bitmap = DEFAULT_MODPACK_ICON,
                    contentDescription = "Modpack Icon",
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialColor.GRAY_200.color, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .padding(8.dp)
                )
                Spacer(modifier = Modifier.size(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = packdir.vo.name,
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    Text(
                        text = "版本${packdir.verName}",
                        style = MaterialTheme.typography.body2,
                        color = MaterialColor.GRAY_700.color
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialColor.RED_900.color,
                        contentColor = Color.White
                    )
                ) {
                    Text("\uEA81 删除".asIconText)
                }
                Button(
                    onClick = onReinstall,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialColor.PINK_900.color,
                        contentColor = Color.White
                    )
                ) {
                    Text("\uDB81\uDC53 重装".asIconText)
                }
                Button(onClick = onOpenFolder) {
                    Text("\uEAED 打开文件夹".asIconText)
                }
            }
        }
    }
}
