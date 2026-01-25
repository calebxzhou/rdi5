package calebxzhou.rdi.client.ui2.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import calebxzhou.rdi.client.AppConfig
import calebxzhou.rdi.client.ui2.BottomSnakebar
import calebxzhou.rdi.client.ui2.CircleIconButton
import calebxzhou.rdi.client.ui2.MainBox
import calebxzhou.rdi.client.ui2.MainColumn
import calebxzhou.rdi.client.ui2.MaterialColor
import calebxzhou.rdi.client.ui2.Space8h
import calebxzhou.rdi.client.ui2.TitleRow
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
fun SettingScreen(onBack: () -> Unit){
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()
    val config = remember { AppConfig.load() }
    val totalMemoryMb = remember { getTotalPhysicalMemoryMb() }
    var useMirror by remember { mutableStateOf(config.useMirror) }
    var maxMemoryText by remember { mutableStateOf(if (config.maxMemory <= 0) "" else config.maxMemory.toString()) }
    var jre21Path by remember { mutableStateOf(config.jre21Path.orEmpty()) }
    var jre8Path by remember { mutableStateOf(config.jre8Path.orEmpty()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

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
                            jre8Path = jre8
                        )
                        AppConfig.save(next)
                        errorMessage = null
                        scaffoldState.snackbarHostState.showSnackbar("设置已保存")
                        saving = false
                    }
                }

            }
            Space8h()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = useMirror, onCheckedChange = { useMirror = it })
                Text("使用镜像源（下载更快）")
            }
            Space8h()
            OutlinedTextField(
                label = { Text("限制MC可用内存 (MB，0 或空为不限制)") },
                value = maxMemoryText,
                onValueChange = { maxMemoryText = it.trim() },
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
                onValueChange = { jre21Path = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Space8h()
            OutlinedTextField(
                label = { Text("Java8主程序路径（可选 留空自带）") },
                value = jre8Path,
                onValueChange = { jre8Path = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Space8h()
            errorMessage?.let { Text(it, color = MaterialTheme.colors.error) }
        }
        BottomSnakebar(scaffoldState.snackbarHostState)
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
