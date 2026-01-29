package calebxzhou.rdi.client.ui2.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import calebxzhou.rdi.client.AppConfig
import calebxzhou.rdi.client.net.loggedAccount
import calebxzhou.rdi.client.net.server
import calebxzhou.rdi.client.service.GameService
import calebxzhou.rdi.client.service.ModpackService
import calebxzhou.rdi.client.service.ModpackService.startInstall
import calebxzhou.rdi.client.service.PlayerInfoCache
import calebxzhou.rdi.client.service.PlayerService
import calebxzhou.rdi.client.ui2.*
import calebxzhou.rdi.client.ui2.comp.McVersionCard
import calebxzhou.rdi.client.ui2.comp.ModpackManageCard
import calebxzhou.rdi.client.ui2.comp.PasswordField
import calebxzhou.rdi.common.model.McVersion
import calebxzhou.rdi.common.model.Modpack
import calebxzhou.rdi.common.model.Task
import calebxzhou.rdi.common.model.TaskProgress
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File
import java.io.FileOutputStream
import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * calebxzhou @ 2026-01-24 18:36
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    onBack: () -> Unit,
    onOpenTask: ((Task) -> Unit)? = null
){
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()
    val config = remember { AppConfig.load() }
    val totalMemoryMb = remember { getTotalPhysicalMemoryMb() }
    var category by remember { mutableStateOf(SettingCategory.Account) }
    var useMirror by remember { mutableStateOf(config.useMirror) }
    var maxMemoryText by remember { mutableStateOf(if (config.maxMemory <= 0) "" else config.maxMemory.toString()) }
    var jre21Path by remember { mutableStateOf(config.jre21Path.orEmpty()) }
    var jre8Path by remember { mutableStateOf(config.jre8Path.orEmpty()) }
    var carrier by remember { mutableStateOf(config.carrier) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var showChangeProfile by remember { mutableStateOf(false) }

    MainBox {
        MainColumn {
            TitleRow("设置",onBack){
                CircleIconButton(
                    icon = "\uF0C7",
                    tooltip = "保存",
                    bgColor = MaterialColor.GREEN_900.color,
                    enabled = !saving
                ) {
                    if (saving) return@CircleIconButton
                    saving = true
                    scope.launch {
                        val memoryValue = maxMemoryText.trim().takeIf { it.isNotEmpty() }?.toIntOrNull()
                        if (maxMemoryText.isNotBlank() && memoryValue == null) {
                            errorMessage = "最大内存格式不正确"
                            saving = false
                            return@launch
                        }
                        if (memoryValue != null && memoryValue != 0) {
                            if (memoryValue <= 4096) {
                                errorMessage = "最大内存必须大于4096MB"
                                saving = false
                                return@launch
                            }
                            if (totalMemoryMb > 0 && memoryValue >= totalMemoryMb) {
                                errorMessage = "最大内存必须小于总内存 ${totalMemoryMb}MB"
                                saving = false
                                return@launch
                            }
                        }
                        val jre21 = jre21Path.trim().takeIf { it.isNotEmpty() }
                        val jre8 = jre8Path.trim().takeIf { it.isNotEmpty() }
                        val java21Ok = withContext(Dispatchers.IO) {
                            jre21?.let { validateJavaPath(it, 21) } ?: Result.success(Unit)
                        }
                        val java8Ok = withContext(Dispatchers.IO) {
                            jre8?.let { validateJavaPath(it, 8) } ?: Result.success(Unit)
                        }
                        java21Ok.exceptionOrNull()?.let {
                            errorMessage = it.message ?: "Java 21 路径无效"
                            saving = false
                            return@launch
                        }
                        java8Ok.exceptionOrNull()?.let {
                            errorMessage = it.message ?: "Java 8 路径无效"
                            saving = false
                            return@launch
                        }
                        val next = AppConfig(
                            useMirror = useMirror,
                            maxMemory = memoryValue ?: 0,
                            jre21Path = jre21,
                            jre8Path = jre8,
                            carrier = carrier
                        )
                        AppConfig.save(next)
                        errorMessage = null
                        scaffoldState.snackbarHostState.showSnackbar("设置已保存")
                        saving = false
                    }
                }
            }
            Space8h()
            Row(modifier = Modifier.fillMaxSize()) {
                SettingNav(
                    selected = category,
                    onSelect = { category = it }
                )
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        when (category) {
                            SettingCategory.Account -> {
                                AccountSettings(
                                    onChangeProfile = { showChangeProfile = true }
                                )
                            }
                            SettingCategory.Java -> {
                                JavaSettings(
                                    totalMemoryMb = totalMemoryMb,
                                    maxMemoryText = maxMemoryText,
                                    onMaxMemoryChange = { maxMemoryText = it.trim() },
                                    jre21Path = jre21Path,
                                    onJre21Change = { jre21Path = it },
                                    jre8Path = jre8Path,
                                    onJre8Change = { jre8Path = it }
                                )
                            }
                            SettingCategory.Mc -> {
                                McSettings(
                                    onOpenTask = onOpenTask
                                )
                            }
                            SettingCategory.Network -> {
                                NetworkSettings(
                                    useMirror = useMirror,
                                    onUseMirrorChange = { useMirror = it },
                                    carrier = carrier,
                                    onCarrierChange = { carrier = it }
                                )
                            }
                        }
                        errorMessage?.let {
                            Text(
                                it,
                                color = MaterialTheme.colors.error,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
        BottomSnakebar(scaffoldState.snackbarHostState)
        if (showChangeProfile) {
            ChangeProfileDialog(
                onDismiss = { showChangeProfile = false },
                onSuccess = {
                    showChangeProfile = false
                    scope.launch {
                        scaffoldState.snackbarHostState.showSnackbar("修改成功")
                    }
                }
            )
        }
    }
}

private enum class SettingCategory(val label: String) {
    Account("\uEB99 账号"),
    Java("\uE738 Java"),
    Mc("\uEC17 MC"),
    Network("\uEF09 网络")
}

@Composable
private fun SettingNav(
    selected: SettingCategory,
    onSelect: (SettingCategory) -> Unit
) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .fillMaxHeight()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SettingCategory.entries.forEach { category ->
            val isSelected = category == selected
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(category) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(20.dp)
                        .background(if (isSelected) MaterialColor.BLUE_500.color else Color.Transparent)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = category.label.asIconText,
                    color = if (isSelected) MaterialColor.BLUE_500.color else Color.Unspecified,
                    style = if (isSelected) MaterialTheme.typography.h6 else MaterialTheme.typography.body1
                )
            }
        }
    }
}

@Composable
private fun AccountSettings(
    onChangeProfile: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("账号信息", style = MaterialTheme.typography.h6)
        Space8h()
        Text("QQ：${loggedAccount.qq}")
        Text("昵称：${loggedAccount.name}")
        Space8h()
        TextButton(onClick = onChangeProfile) {
            Text("修改个人信息")
        }
    }
}

@Composable
private fun JavaSettings(
    totalMemoryMb: Int,
    maxMemoryText: String,
    onMaxMemoryChange: (String) -> Unit,
    jre21Path: String,
    onJre21Change: (String) -> Unit,
    jre8Path: String,
    onJre8Change: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            label = { Text("限制MC可用内存 (MB，0 或空为不限制)") },
            value = maxMemoryText,
            onValueChange = onMaxMemoryChange,
            singleLine = true,
            modifier = Modifier.width(260.dp)
        )
        if (totalMemoryMb > 0) {
            Text("总内存 ${totalMemoryMb}MB", style = MaterialTheme.typography.caption)
        }
        Space8h()
        OutlinedTextField(
            label = { Text("Java21主程序路径（可选 留空自带）") },
            value = jre21Path,
            onValueChange = onJre21Change,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Space8h()
        OutlinedTextField(
            label = { Text("Java8主程序路径（可选 留空自带）") },
            value = jre8Path,
            onValueChange = onJre8Change,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun McSettings(
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

    Column(modifier = Modifier.fillMaxWidth()) {
        Row {
            Text("MC版本资源管理", style = MaterialTheme.typography.subtitle1)
            SpacerFullW()
            CircleIconButton("\uEE38","导入MC运行资源包"){
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
            Text("尚未安装整合包。点击右上角\uDB86\uDDD5按钮下载想玩的包。".asIconText, color = Color.Black)
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
                    onDelete = { confirmDelete = packdir },
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
    }
/*
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
    }*/
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

@Composable
private fun NetworkSettings(
    useMirror: Boolean,
    onUseMirrorChange: (Boolean) -> Unit,
    carrier: Int,
    onCarrierChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = useMirror, onCheckedChange = onUseMirrorChange)
            Text("使用镜像源（下载更快）")
        }
        Space8h()
        CarrierSelector(
            selected = carrier,
            onSelect = onCarrierChange
        )
    }
}

private fun getTotalPhysicalMemoryMb(): Int {
    val osBean = runCatching {
        ManagementFactory.getOperatingSystemMXBean()
    }.getOrNull()
    val totalBytes = (osBean as? com.sun.management.OperatingSystemMXBean)
        ?.totalPhysicalMemorySize
        ?: return 0
    return (totalBytes / (1024L * 1024L)).toInt()
}

private fun validateJavaPath(rawPath: String, expectedMajor: Int): Result<Unit> {
    val resolved = resolveJavaExecutable(rawPath) ?: return Result.failure(
        IllegalStateException("Java 路径无效: $rawPath")
    )
    val version = readJavaMajorVersion(resolved) ?: return Result.failure(
        IllegalStateException("无法识别 Java 版本: ${resolved.absolutePath}")
    )
    if (version != expectedMajor) {
        return Result.failure(
            IllegalStateException("Java 版本应为 $expectedMajor，当前为 $version")
        )
    }
    return Result.success(Unit)
}

private fun resolveJavaExecutable(rawPath: String): File? {
    val input = File(rawPath.trim())
    if (input.isDirectory) {
        val exe = input.resolve("bin").resolve(if (isWindows()) "java.exe" else "java")
        return exe.takeIf { it.exists() }
    }
    if (input.exists()) {
        val name = input.name.lowercase()
        if (isWindows()) {
            return input.takeIf { name == "java.exe" }
        }
        return input.takeIf { name == "java" }
    }
    val exe = File(rawPath.trim() + if (isWindows()) ".exe" else "")
    return exe.takeIf { it.exists() }
}

private fun readJavaMajorVersion(javaExe: File): Int? {
    return runCatching {
        val process = ProcessBuilder(javaExe.absolutePath, "-version")
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()
        val match = Regex("version \"([0-9]+)(?:\\.([0-9]+))?.*\"").find(output)
            ?: return@runCatching null
        val major = match.groupValues.getOrNull(1)?.toIntOrNull() ?: return@runCatching null
        if (major == 1) {
            match.groupValues.getOrNull(2)?.toIntOrNull()
        } else {
            major
        }
    }.getOrNull()
}

private fun isWindows(): Boolean {
    return System.getProperty("os.name").contains("windows", ignoreCase = true)
}

@Composable
private fun CarrierSelector(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    val carriers = listOf("电信", "移动", "联通", "教育网", "广电")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("运营商节点")
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
}

@Composable
private fun ChangeProfileDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var showPassword by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val account = loggedAccount
    var name by remember { mutableStateOf(account.name) }
    var pwd by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }

    androidx.compose.material.AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = { Text("修改信息") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("昵称") },
                    singleLine = true,
                    enabled = !submitting,
                    modifier = Modifier.fillMaxWidth()
                )
                PasswordField(
                    value = pwd,
                    onValueChange = { pwd = it },
                    label = "新密码 留空则不修改",
                    enabled = !submitting,
                    showPassword = showPassword,
                    onToggleVisibility = { showPassword = !showPassword },
                    onEnter = {}
                )
                errorMessage?.let { Text(it, color = MaterialTheme.colors.error) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !submitting,
                onClick = {
                    val nameBytes = name.toByteArray(Charsets.UTF_8).size
                    if (nameBytes !in 3..24) {
                        errorMessage = "昵称须在3~24个字节，当前为${nameBytes}"
                        return@TextButton
                    }
                    if (pwd.isNotEmpty() && pwd.length !in 6..16) {
                        errorMessage = "密码长度须在6~16个字符"
                        return@TextButton
                    }
                    val params = mutableMapOf<String, Any>()
                    if (name != account.name) params["name"] = name
                    if (pwd.isNotEmpty() && pwd != account.pwd) params["pwd"] = pwd
                    if (params.isEmpty()) {
                        errorMessage = "没有修改内容"
                        return@TextButton
                    }
                    submitting = true
                    errorMessage = null
                    scope.launch {
                        runCatching {
                            server.makeRequest<Unit>("player/profile", HttpMethod.Put, params)
                            loggedAccount = PlayerService.login(account.qq, pwd).getOrThrow()
                            PlayerInfoCache -= loggedAccount._id
                        }.getOrElse {
                            errorMessage = "修改失败: ${it.message}"
                            return@launch
                        }
                        submitting = false
                        onSuccess()
                    }
                }
            ) {
                if (submitting) {
                    CircularProgressIndicator(modifier = Modifier.width(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("修改")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { if (!submitting) onDismiss() }) {
                Text("取消")
            }
        }
    )
}
