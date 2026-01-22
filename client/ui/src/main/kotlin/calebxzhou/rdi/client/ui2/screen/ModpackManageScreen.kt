package calebxzhou.rdi.client.ui2.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
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
import calebxzhou.rdi.client.ui2.*
import calebxzhou.rdi.client.ui2.comp.HttpImage
import calebxzhou.rdi.common.model.McVersion
import calebxzhou.rdi.common.model.Modpack
import calebxzhou.rdi.common.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import java.awt.Desktop
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * calebxzhou @ 2026-01-13 23:14
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModpackManageScreen(
    onBack: () -> Unit = {},
    onOpenModpackList: (() -> Unit)? = null,
    onOpenTask: ((Task) -> Unit)? = null,
    onOpenMcPlay: ((McPlayArgs) -> Unit)? = null
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
                McVersionCard(
                    mcver = mcver,
                    onOpenTask = onOpenTask
                )
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
            Text("尚未安装整合包。点击右上角\uDB86\uDDD5按钮下载想玩的包。".asIconText, color = Color.Black)
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(280.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(localDirs, key = { "${it.vo.id}_${it.verName}" }) { packdir ->
                val versionId = "${packdir.vo.id}_${packdir.verName}"
                val runningArgs = McPlayStore.current
                val isRunning = runningArgs?.versionId == versionId && McPlayStore.process?.isAlive == true
                ModpackManageCard(
                    packdir = packdir,
                    isRunning = isRunning,
                    onDelete = { confirmDelete = packdir },
                    onReinstall = {
                        scope.launch {
                            val version = server.makeRequest<Modpack.Version>("modpack/${packdir.vo.id}/version/${packdir.verName}").data
                            if (version == null) {
                                errorMessage = "未找到对应版本信息，可能已被删除"
                                return@launch
                            }
                            val task = version.startInstall(packdir.vo.mcVer, packdir.vo.modloader, packdir.vo.name)
                            if (onOpenTask != null) {
                                onOpenTask(task)
                            } else {
                                errorMessage = "暂不支持在此页面下载"
                            }
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
                    },
                    onExportLogs = {
                        scope.launch {
                            val result = exportLogsPack(packdir)
                            if (result.isFailure) {
                                errorMessage = result.exceptionOrNull()?.message ?: "导出日志失败"
                            }
                        }
                    },
                    onOpenMcPlay = if (isRunning && runningArgs != null) {
                        { onOpenMcPlay?.invoke(runningArgs) }
                    } else {
                        null
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
fun McVersionCard(
    mcver: McVersion,
    onOpenTask: ((Task) -> Unit)? = null
) {
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
                        onOpenTask?.invoke(GameService.downloadVersion(mcver,mcver.firstLoader))
                    }
                    CircleIconButton("\uF305", "仅下载MC核心", bgColor = Color.Gray) {
                        onOpenTask?.invoke(GameService.downloadClient(mcver.metadata))
                    }
                    CircleIconButton("\uDB84\uDE5F", "仅下载运行库", bgColor = Color.Gray) {
                        onOpenTask?.invoke(GameService.downloadLibraries(mcver.metadata.libraries))
                    }
                    CircleIconButton("\uF001", "仅下载音频资源", bgColor = Color.Gray) {
                        onOpenTask?.invoke(GameService.downloadAssets(mcver.metadata))
                    }
                    mcver.loaderVersions.forEach { (loader, _) ->
                        CircleIconButton("\uEEFF", "安装${loader.name.lowercase()}", bgColor = Color.Gray) {
                            onOpenTask?.invoke(GameService.downloadLoader(mcver,loader))
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
    isRunning: Boolean = false,
    onDelete: () -> Unit,
    onReinstall: () -> Unit,
    onOpenFolder: () -> Unit,
    onExportLogs: () -> Unit,
    onOpenMcPlay: (() -> Unit)? = null
) {

    val cardShape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    val glowModifier = if (isRunning) {
        Modifier
            .background(Color(0xFFE9D5FF), cardShape)
            .padding(2.dp)
    } else {
        Modifier
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(glowModifier)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFF9F9FB),
            shape = cardShape,
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
                    val iconUrl = packdir.vo.icon?.takeIf { it.isNotBlank() }
                    if (iconUrl != null) {
                        HttpImage(
                            imgUrl = iconUrl,
                            modifier = Modifier.fillMaxSize(),
                            contentDescription = "Modpack Icon"
                        )
                    } else {
                        Image(
                            bitmap = DEFAULT_MODPACK_ICON,
                            contentDescription = "Modpack Icon",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
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
                        CircleIconButton(
                            "\uEF11",
                            "导出日志",
                            bgColor = MaterialColor.GRAY_900.color,
                        ) {
                            onExportLogs()
                        }
                        if (isRunning && onOpenMcPlay != null) {
                            CircleIconButton(
                                "\uF120",
                                "控制台",
                                bgColor = MaterialColor.BLUE_800.color,
                            ) {
                                onOpenMcPlay()
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun exportLogsPack(packdir: ModpackService.LocalDir): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
        val sourceDirs = listOf("logs", "crash-reports")
            .map { packdir.dir.resolve(it) }
            .filter { it.exists() && it.isDirectory }
        if (sourceDirs.isEmpty()) {
            throw IllegalStateException("没有可导出的日志目录")
        }

        val chooser = JFileChooser().apply {
            dialogTitle = "选择日志保存位置"
            fileSelectionMode = JFileChooser.FILES_ONLY
            val safeName = packdir.vo.name.ifBlank { "modpack" }.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            val defaultName = "${safeName}_${packdir.verName}_logs.zip"
            selectedFile = java.io.File(System.getProperty("user.home"), defaultName)
            fileFilter = FileNameExtensionFilter("ZIP 文件 (*.zip)", "zip")
        }
        val result = chooser.showSaveDialog(null)
        if (result != JFileChooser.APPROVE_OPTION) return@runCatching

        var outputFile = chooser.selectedFile
        if (!outputFile.name.endsWith(".zip", ignoreCase = true)) {
            outputFile = java.io.File(outputFile.parentFile, "${outputFile.name}.zip")
        }

        ZipOutputStream(FileOutputStream(outputFile)).use { zipOut ->
            sourceDirs.forEach { dir ->
                val baseName = dir.name
                dir.walkTopDown()
                    .filter { it.isFile }
                    .forEach { file ->
                        val relative = file.relativeTo(dir).invariantSeparatorsPath
                        val entryName = "$baseName/$relative"
                        zipOut.putNextEntry(ZipEntry(entryName))
                        file.inputStream().use { it.copyTo(zipOut) }
                        zipOut.closeEntry()
                    }
            }
        }
    }
}
