package calebxzhou.rdi.client.ui2.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import calebxzhou.mykotutils.std.deleteRecursivelyNoSymlink
import calebxzhou.rdi.client.net.server
import calebxzhou.rdi.client.service.GameService
import calebxzhou.rdi.client.service.ModpackService
import calebxzhou.rdi.client.service.ModpackService.startInstall
import calebxzhou.rdi.client.ui2.*
import calebxzhou.rdi.client.ui2.comp.McVersionCard
import calebxzhou.rdi.client.ui2.comp.ModpackManageCard
import calebxzhou.rdi.common.model.McVersion
import calebxzhou.rdi.common.model.Modpack
import calebxzhou.rdi.common.model.Task
import calebxzhou.rdi.common.model.TaskProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * calebxzhou @ 2026-01-29 18:43
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McVersionScreen(
    onBack: () -> Unit,
    requiredMcVer: McVersion? = null,
    onOpenTask: ((Task) -> Unit)? = null
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

    MainBox {
        MainColumn {
            TitleRow("MC资源管理", onBack) {
                Text("若下载不成功，可尝试从网盘下载，然后手动导入。")
                CircleIconButton("\uDB85\uDC03","从网盘下载"){
                    val url = "https://www.123865.com/s/iWSWvd-Zrtdd"
                    runCatching {
                        if (Desktop.isDesktopSupported()) {
                            Desktop.getDesktop().browse(URI(url))
                        } else {
                            ProcessBuilder("explorer", url).start()
                        }
                    }.onFailure {
                        errorMessage = "无法打开浏览器: ${it.message}"
                    }
                }
                Space8w()
                CircleIconButton("\uEE38", "导入MC运行资源包") {
                    val files = selectRdiPackFiles() ?: return@CircleIconButton
                    val task = if (files.size == 1) {
                        buildImportPackTask(files.first())
                    } else {
                        Task.Sequence(
                            name = "导入整合包",
                            subTasks = files.map { buildImportPackTask(it) }
                        )
                    }
                    onOpenTask?.invoke(task)
                }
            }
            Space8h()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                if (requiredMcVer != null) {
                    Text(
                        text = "请先下载所需版本：${requiredMcVer.mcVer}",
                        color = MaterialTheme.colors.error
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(300.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(min = 0.dp, max = 800.dp)
                ) {
                    items(McVersion.entries) { mcver ->
                        McVersionCard(
                            mcver = mcver,
                            highlight = requiredMcVer == mcver,
                            onOpenTask = onOpenTask
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("已安装整合包", style = MaterialTheme.typography.subtitle1)
                Spacer(modifier = Modifier.height(8.dp))
                if (loading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                if (!loading && localDirs.isEmpty()) {
                    Text("尚未安装整合包。".asIconText, color = Color.Black)
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(280.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(min = 0.dp, max = 1200.dp)
                ) {
                    items(localDirs, key = { "${it.vo.id}_${it.verName}" }) { packdir ->
                        val versionId = "${packdir.vo.id}_${packdir.verName}"
                        val runningArgs = McPlayStore.current
                        val isRunning = runningArgs?.versionId == versionId && McPlayStore.process?.isAlive == true
                        ModpackManageCard(
                            packdir = packdir,
                            isRunning = isRunning,
                            onDelete = {
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        runCatching { packdir.dir.deleteRecursivelyNoSymlink() }
                                    }
                                    if (result.isFailure) {
                                        errorMessage = "删除失败: ${result.exceptionOrNull()?.message}"
                                    }
                                    reload()
                                }
                            },
                            onReinstall = {
                                scope.launch {
                                    val version = server.makeRequest<Modpack.Version>(
                                        "modpack/${packdir.vo.id}/version/${packdir.verName}"
                                    ).data
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
                            onOpenMcPlay = null
                        )
                    }
                }
                errorMessage?.let { Text(it, color = MaterialTheme.colors.error) }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private fun selectRdiPackFiles(): List<File>? {
    val chooser = JFileChooser().apply {
        dialogTitle = "选择资源包 (*.mc.rdipack)"
        fileSelectionMode = JFileChooser.FILES_ONLY
        isMultiSelectionEnabled = true
        currentDirectory = File("C:/Users/${System.getProperty("user.name")}/Downloads")
        fileFilter = FileNameExtensionFilter("RDI资源包 (*.rdipack)", "rdipack")
    }
    val result = chooser.showOpenDialog(null)
    if (result != JFileChooser.APPROVE_OPTION) return null
    val files = chooser.selectedFiles?.toList()
        ?: chooser.selectedFile?.let { listOf(it) }
        ?: return null
    return files.filter { it.exists() && it.isFile && it.name.endsWith(".rdipack", ignoreCase = true) }
        .takeIf { it.isNotEmpty() }
}

private fun buildImportPackTask(zipFile: File): Task {
    return Task.Leaf("导入 ${zipFile.name}") { ctx ->
        val targetRoot = GameService.DIR.canonicalFile
        val totalFiles = ZipFile(zipFile).use { zip ->
            zip.entries().asSequence().count { !it.isDirectory }
        }.coerceAtLeast(1)
        var processed = 0
        ZipFile(zipFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val name = entry.name.replace('\\', '/').trimStart('/')
                if (name.isEmpty()) return@forEach
                val outFile = targetRoot.resolve(name)
                val normalized = outFile.canonicalFile
                if (!normalized.path.startsWith(targetRoot.path)) return@forEach
                if (entry.isDirectory) {
                    normalized.mkdirs()
                } else {
                    normalized.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input ->
                        Files.newOutputStream(
                            normalized.toPath(),
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING
                        ).use { output -> input.copyTo(output) }
                    }
                    processed += 1
                    ctx.emitProgress(
                        TaskProgress("解压 ${entry.name}", processed.toFloat() / totalFiles)
                    )
                }
            }
        }
        ctx.emitProgress(TaskProgress("完成", 1f))
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
