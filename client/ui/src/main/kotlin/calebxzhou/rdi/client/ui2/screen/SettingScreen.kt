package calebxzhou.rdi.client.ui2.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import calebxzhou.mykotutils.hwspec.HwSpec
import calebxzhou.mykotutils.std.humanFileSize
import calebxzhou.rdi.client.AppConfig
import calebxzhou.rdi.client.net.loggedAccount
import calebxzhou.rdi.client.net.server
import calebxzhou.rdi.client.service.PlayerInfoCache
import calebxzhou.rdi.client.service.PlayerService
import calebxzhou.rdi.client.ui2.*
import calebxzhou.rdi.client.ui2.comp.PasswordField
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.management.ManagementFactory

/**
 * calebxzhou @ 2026-01-24 18:36
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(
    onBack: () -> Unit
) {
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
            TitleRow("设置", onBack) {
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


private enum class SettingCategory(val icon: String, val label: String) {
    Account("\uEB99", "账号"),
    Java("\uE738", "Java"),
    Network("\uEF09", "网络")
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
                        .background(if (isSelected) MaterialTheme.colors.primary else Color.Transparent)
                )
                Space8w()
                Text(
                    text = category.icon.asIconText,
                    color = if (isSelected) MaterialTheme.colors.primary else Color.Unspecified,
                    style = when (category) {
                        //java图标比较小 放大一些
                        SettingCategory.Java -> MaterialTheme.typography.h5
                        else -> MaterialTheme.typography.subtitle1
                    }

                )
                Space8w()
                Text(
                    text = category.label,
                    color = if (isSelected) MaterialTheme.colors.primary else Color.Unspecified,
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
        Text("内存信息 总可用 ${totalMemoryMb}MB")
        Space8h()
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(end = 16.dp)) {
                OutlinedTextField(
                    label = { Text("限制MC可用内存 (MB，0 或空为不限制)") },
                    value = maxMemoryText,
                    onValueChange = onMaxMemoryChange,
                    singleLine = true,
                    modifier = Modifier.width(260.dp)
                )
            }
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalArrangement = Arrangement.Top,
                maxItemsInEachRow = 2
            ) {
                HwSpec.NOW.mems.forEach { mem ->
                    Row(
                        modifier = Modifier
                            .padding(start = 12.dp, bottom = 6.dp)
                            .widthIn(min = 160.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("\uEFC5".asIconText)
                        Space8w()
                        Text("${mem.size.humanFileSize}  ${mem.type}  ${mem.speed / 1024 / 1024}MHz")
                    }
                }
            }
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

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            carriers.forEachIndexed { index, name ->
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
                            if (pwd.isNotEmpty()) {
                                loggedAccount = PlayerService.login(account._id.toHexString(), pwd).getOrThrow()
                            }
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
