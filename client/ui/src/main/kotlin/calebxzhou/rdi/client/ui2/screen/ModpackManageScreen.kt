package calebxzhou.rdi.client.ui2.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import calebxzhou.mykotutils.std.deleteRecursivelyNoSymlink
import calebxzhou.mykotutils.std.jarResource
import calebxzhou.rdi.RDIClient
import calebxzhou.rdi.client.model.firstLoader
import calebxzhou.rdi.client.model.metadata
import calebxzhou.rdi.client.net.server
import calebxzhou.rdi.client.service.GameService
import calebxzhou.rdi.client.service.ModpackService
import calebxzhou.rdi.client.service.ModpackService.startInstall
import calebxzhou.rdi.client.ui.frag.TaskFragment
import calebxzhou.rdi.client.ui.go
import calebxzhou.rdi.client.ui2.CircleIconButton
import calebxzhou.rdi.client.ui2.DEFAULT_MODPACK_ICON
import calebxzhou.rdi.client.ui2.MainColumn
import calebxzhou.rdi.client.ui2.MaterialColor
import calebxzhou.rdi.client.ui2.TitleRow
import calebxzhou.rdi.client.ui2.asIconText
import calebxzhou.rdi.client.ui2.hM
import calebxzhou.rdi.common.model.McVersion
import calebxzhou.rdi.common.model.Modpack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import java.awt.Desktop
import kotlin.collections.component1
import kotlin.collections.component2

/**
 * calebxzhou @ 2026-01-13 23:14
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModpackManageScreen(
    onBack: () -> Unit = {},
    onOpenModpackList: (() -> Unit)? = null
) {
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

    MainColumn {
        // --- MC Version Management Section ---
        TitleRow("MC资源·整合包管理", onBack) {
            CircleIconButton("\uDB86\uDDD5", "整合包列表") {
                onOpenModpackList?.invoke()
            }
        }
        Spacer(16.hM)
        LazyVerticalGrid(
            columns = GridCells.Adaptive(300.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(McVersion.entries) { mcver ->
                McVersionCard(mcver = mcver)
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
        Spacer(16.hM)
        if (!loading && localDirs.isEmpty()) {
            Text("尚未安装本地整合包", color = Color.Black)
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(280.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
                            version.startInstall(packdir.vo.mcVer, packdir.vo.modloader, packdir.vo.name)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McVersionCard(mcver: McVersion) {
    val iconBitmap = remember(mcver) {
        RDIClient.jarResource(mcver.icon).use { stream ->
            Image.makeFromEncoded(stream.readBytes()).toComposeImageBitmap()
        }
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFF9F9FB),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialColor.GRAY_200.color, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = "MC ${mcver.mcVer}",
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "MC ${mcver.mcVer}",
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialColor.GRAY_900.color
                    )
                    Text(
                        text = mcver.loaderVersions.keys.joinToString(" / ") { it.name.lowercase() },
                        style = MaterialTheme.typography.body2,
                        color = MaterialColor.GRAY_700.color
                    )
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircleIconButton("\uF019", "下载全部所需文件") {
                        /*TaskFragment("下载MC文件") {
                            GameService.downloadVersion(mcver) { log(it) }
                            GameService.downloadLoader(mcver, mcver.firstLoader) { log(it) }
                        }.go()*/
                    }
                    CircleIconButton("\uF305", "仅下载MC核心") {
                        /*TaskFragment("下载MC核心文件") {
                            GameService.downloadClient(mcver.metadata) { log(it) }
                        }.go()*/
                    }
                    CircleIconButton("\uDB84\uDE5F", "仅下载运行库") {
                        /*TaskFragment("下载运行库文件") {
                            GameService.downloadLibraries(mcver.metadata) { log(it) }
                        }.go()*/
                    }
                    CircleIconButton("\uF001", "仅下载音频资源") {
                        /* TaskFragment("下载音频资源文件") {
                             GameService.downloadAssets(mcver.metadata) { log(it) }
                         }.go()*/
                    }
                    mcver.loaderVersions.forEach { (loader, _) ->
                        CircleIconButton("\uEEFF", "安装${loader.name.lowercase()}") {
                            /* TaskFragment("下载${loader}文件") {
                                 GameService.downloadLoader(mcver, loader) { log(it) }
                             }.go()*/
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialColor.GRAY_200.color, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = DEFAULT_MODPACK_ICON,
                    contentDescription = "Modpack Icon",
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialColor.GRAY_200.color, androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .padding(4.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = packdir.vo.name ,
                        style = MaterialTheme.typography.subtitle1,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    Text(
                        text = packdir.verName,
                        style = MaterialTheme.typography.body2,
                        color = MaterialColor.GRAY_700.color
                    )
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircleIconButton(
                        "\uEA81",
                        "删除",
                        bgColor = MaterialColor.RED_900.color,
                    ) {
                        onDelete()
                    }
                    CircleIconButton(
                        "\uDB81\uDC53",
                        "重装",
                        bgColor = MaterialColor.PINK_900.color,
                    ) {
                        onReinstall()
                    }
                    CircleIconButton(
                        "\uEAED",
                        "打开文件夹",
                    ) {
                        onOpenFolder ()
                    }
                }
            }
        }

    }
}
