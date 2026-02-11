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
import androidx.compose.ui.Alignment
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
import calebxzhou.rdi.common.DL_MOD_DIR
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
    onOpenTask: ((Task) -> Unit)? = null,
    onOpenPlay: ((McPlayArgs) -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var localDirs by remember { mutableStateOf<List<ModpackService.LocalDir>>(emptyList()) }
    var selectedPack by remember { mutableStateOf<ModpackService.LocalDir?>(null) }
    var packActionMessage by remember { mutableStateOf<String?>(null) }

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
                selectedPack = selectedPack?.let { selected ->
                    localDirs.firstOrNull { it.versionId == selected.versionId }
                }
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
                val selected = selectedPack
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("已安装整合包", style = MaterialTheme.typography.subtitle1)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        packActionMessage?.let {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(it, color = MaterialTheme.colors.primary)
                        }
                        Space8w()
                        selected?.let {
                            Text("已选择 ${it.vo.name} ${it.verName}")
                        }?: Text("选择一个整合包...")
                        Space8w()
                        val size = 32
                        CircleIconButton(
                            "\uDB82\uDD5D",
                            "导入RDI整合包",
                            bgColor = MaterialColor.GREEN_900.color,
                            size = size,
                            enabled = true
                        ) {
                            scope.launch {
                                val task = withContext(Dispatchers.IO) {
                                    runCatching {
                                        packActionMessage = "开始导入..."
                                        importRdiModpack { msg -> packActionMessage = msg }
                                    }
                                }.getOrElse {
                                    errorMessage = it.message ?: "导入失败"
                                    packActionMessage = null
                                    return@launch
                                }
                                if (onOpenTask != null) {
                                    onOpenTask(task)
                                    reload()
                                    packActionMessage = "导入完成"
                                } else {
                                    errorMessage = "暂不支持在此页面下载"
                                    packActionMessage = null
                                }
                            }
                        }
                        CircleIconButton(
                            "\uEA81",
                            "删除",
                            size = size,
                            bgColor = MaterialColor.RED_900.color,
                            longPressDelay = 5000L,
                            enabled = selected != null
                        ) {
                            val packdir = selected ?: return@CircleIconButton
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    runCatching { packdir.dir.deleteRecursivelyNoSymlink() }
                                }
                                if (result.isFailure) {
                                    errorMessage = "删除失败: ${result.exceptionOrNull()?.message}"
                                }
                                reload()
                            }
                        }
                        CircleIconButton(
                            "\uDB81\uDC53",
                            "重装",
                            size = size,
                            bgColor = MaterialColor.PINK_900.color,
                            enabled = selected != null
                        ) {
                            val packdir = selected ?: return@CircleIconButton
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
                        }
                        CircleIconButton(
                            "\uEAED",
                            "打开文件夹",
                            size = size,
                            bgColor = MaterialColor.YELLOW_900.color,
                            enabled = selected != null
                        ) {
                            val packdir = selected ?: return@CircleIconButton
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
                        CircleIconButton("\uEB9B","测试运行",size=size, enabled = selected!=null){
                            selected?.let { packdir->
                                val args = listOf(
                                    "-Drdi.ihq.url=${server.hqUrl}",
                                    "-Drdi.game.ip=${server.ip}:${server.gamePort}",
                                    "-Drdi.host.name=test",
                                    "-Drdi.host.port=25565"
                                )
                                val mcVer = packdir.vo.mcVer
                                val versionId = packdir.versionId
                                val playArgs = McPlayArgs(
                                    title = "测试运行 - ${packdir.vo.name} ${packdir.verName}",
                                    mcVer = mcVer,
                                    versionId = versionId,
                                    jvmArgs = args
                                )
                                onOpenPlay?.invoke(playArgs)
                            }
                        }
                        CircleIconButton(
                            "\uEF11",
                            "导出日志",
                            size = size,
                            bgColor = MaterialColor.GRAY_900.color,
                            enabled = selected != null
                        ) {
                            val packdir = selected ?: return@CircleIconButton
                            scope.launch {
                                val result = exportLogsPack(packdir)
                                if (result.isFailure) {
                                    errorMessage = result.exceptionOrNull()?.message ?: "导出日志失败"
                                }
                            }
                        }

                        CircleIconButton(
                            "\uDB82\uDD5E",
                            "导出RDI整合包",
                            size = size,
                            enabled = selected != null
                        ) {
                            val packdir = selected ?: return@CircleIconButton
                            scope.launch {
                                val result = withContext(Dispatchers.IO) {
                                    packActionMessage = "开始导出..."
                                    exportRdiModpack(packdir) { msg -> packActionMessage = msg }
                                }
                                if (result.isFailure) {
                                    errorMessage = result.exceptionOrNull()?.message ?: "导出失败"
                                    packActionMessage = null
                                } else {
                                    packActionMessage = "导出完成"
                                }
                            }
                        }
                    }
                }

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
                            selected = selectedPack?.versionId == packdir.versionId,
                            onClick = { selectedPack = packdir }
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

private fun selectRdiModpackFile(): File? {
    val chooser = JFileChooser().apply {
        dialogTitle = "选择RDI整合包 (*.rdimodpack)"
        fileSelectionMode = JFileChooser.FILES_ONLY
        isMultiSelectionEnabled = false
        currentDirectory = File("C:/Users/${System.getProperty("user.name")}/Downloads")
        fileFilter = FileNameExtensionFilter("RDI整合包 (*.rdimodpack)", "rdimodpack")
    }
    val result = chooser.showOpenDialog(null)
    if (result != JFileChooser.APPROVE_OPTION) return null
    return chooser.selectedFile
        ?.takeIf { it.exists() && it.isFile && it.name.endsWith(".rdimodpack", ignoreCase = true) }
}

private suspend fun exportRdiModpack(
    packdir: ModpackService.LocalDir,
    onProgress: (String) -> Unit
): Result<Unit> = withContext(Dispatchers.IO) {
    runCatching {
        val packZip = ModpackService.DL_PACKS_DIR.resolve("${packdir.vo.id}_${packdir.verName}.zip")
        if (!packZip.exists()) {
            throw IllegalStateException("整合包文件不存在，请先下载")
        }
        val version = server.makeRequest<Modpack.Version>(
            "modpack/${packdir.vo.id}/version/${packdir.verName}"
        ).data ?: throw IllegalStateException("无法获取整合包版本信息")

        val chooser = JFileChooser().apply {
            dialogTitle = "选择导出位置"
            fileSelectionMode = JFileChooser.FILES_ONLY
            val safeName = packdir.vo.name.ifBlank { "modpack" }.replace(Regex("[\\\\/:*?\"<>|]"), "_")
            val defaultName = "${safeName}_${packdir.verName}.rdimodpack"
            selectedFile = File(System.getProperty("user.home"), defaultName)
            fileFilter = FileNameExtensionFilter("RDI整合包 (*.rdimodpack)", "rdimodpack")
        }
        val result = chooser.showSaveDialog(null)
        if (result != JFileChooser.APPROVE_OPTION) return@runCatching

        var outputFile = chooser.selectedFile
        if (!outputFile.name.endsWith(".rdimodpack", ignoreCase = true)) {
            outputFile = File(outputFile.parentFile, "${outputFile.name}.rdimodpack")
        }

        val missingMods = mutableListOf<String>()
        val total = version.mods.size + 1
        var processed = 0
        ZipOutputStream(FileOutputStream(outputFile)).use { zipOut ->
            zipOut.putNextEntry(ZipEntry(packZip.name))
            packZip.inputStream().use { it.copyTo(zipOut) }
            zipOut.closeEntry()
            processed += 1
            onProgress("导出整合包 ${processed}/${total}")

            version.mods.forEach { mod ->
                val modFile = DL_MOD_DIR.resolve(mod.fileName)
                if (!modFile.exists()) {
                    missingMods += mod.fileName
                    return@forEach
                }
                zipOut.putNextEntry(ZipEntry("mods/${mod.fileName}"))
                modFile.inputStream().use { it.copyTo(zipOut) }
                zipOut.closeEntry()
                processed += 1
                onProgress("导出MOD ${processed}/${total}")
            }
        }

        if (missingMods.isNotEmpty()) {
            runCatching { outputFile.delete() }
            throw IllegalStateException("缺少 ${missingMods.size} 个MOD文件：${missingMods.take(3).joinToString()}${if (missingMods.size > 3) "..." else ""}")
        }
    }
}

private suspend fun importRdiModpack(
    onProgress: (String) -> Unit
): Task = withContext(Dispatchers.IO) {
    val file = selectRdiModpackFile() ?: throw IllegalStateException("未选择整合包文件")
    val packZipName: String
    ZipFile(file).use { zip ->
        val packEntry = zip.entries().asSequence()
            .firstOrNull { !it.isDirectory && it.name.endsWith(".zip", ignoreCase = true) && !it.name.startsWith("mods/") }
            ?: throw IllegalStateException("整合包内未找到modpack.zip")

        packZipName = File(packEntry.name).name
        val packTarget = ModpackService.DL_PACKS_DIR.resolve(packZipName)
        packTarget.parentFile?.mkdirs()
        val modEntries = zip.entries().asSequence()
            .filter { !it.isDirectory && it.name.startsWith("mods/") }
            .toList()
        val total = modEntries.size + 1
        var processed = 0
        zip.getInputStream(packEntry).use { input ->
            Files.newOutputStream(
                packTarget.toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
            ).use { output -> input.copyTo(output) }
        }
        processed += 1
        onProgress("导入整合包 ${processed}/${total}")

        modEntries.forEach { entry ->
            val filename = entry.name.substringAfter("mods/").trim()
            if (filename.isBlank()) return@forEach
            val target = DL_MOD_DIR.resolve(filename)
            target.parentFile?.mkdirs()
            zip.getInputStream(entry).use { input ->
                Files.newOutputStream(
                    target.toPath(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
                ).use { output -> input.copyTo(output) }
            }
            processed += 1
            onProgress("导入MOD ${processed}/${total}")
        }
    }

    val match = Regex("^([0-9a-fA-F]{24})_(.+)\\.zip$").find(packZipName)
        ?: throw IllegalStateException("整合包文件名无效：$packZipName")
    val (idStr, verName) = match.destructured
    val modpackId = org.bson.types.ObjectId(idStr)
    val modpackVo = server.makeRequest<Modpack.BriefVo>("modpack/${modpackId}/brief").data
        ?: throw IllegalStateException("未找到整合包信息")
    val version = server.makeRequest<Modpack.Version>("modpack/${modpackId}/version/${verName}").data
        ?: throw IllegalStateException("未找到整合包版本信息")
    version.startInstall(modpackVo.mcVer, modpackVo.modloader, modpackVo.name)
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
