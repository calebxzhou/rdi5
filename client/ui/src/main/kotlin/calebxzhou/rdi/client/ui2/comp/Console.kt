package calebxzhou.rdi.client.ui2.comp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import calebxzhou.rdi.client.ui2.CircleIconButton
import calebxzhou.rdi.client.CodeFontFamily
import calebxzhou.rdi.client.ui2.MaterialColor
import calebxzhou.rdi.client.ui2.alertErr
import calebxzhou.rdi.client.ui2.alertOk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

class ConsoleState(
    private val maxLogLines: Int = 500
) {
    private val _lines: SnapshotStateList<String> = mutableStateListOf()
    val lines: List<String> get() = _lines

    fun append(line: String) {
        val trimmed = line.trimEnd('\r')
        if (trimmed.isBlank()) return
        _lines.add(trimmed)
        while (_lines.size > maxLogLines) {
            _lines.removeAt(0)
        }
    }

    fun appendAll(items: Iterable<String>) {
        items.forEach { append(it) }
    }

    fun clear() {
        _lines.clear()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Console(
    state: ConsoleState,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val lineCount = state.lines.size
    val scope = rememberCoroutineScope()
    var showSuccess by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(lineCount) {
        if (lineCount > 0) {
            listState.animateScrollToItem(lineCount - 1)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 12.dp)
        ) {
            items(state.lines.size) { index ->
                Text(
                    text = formatLine(state.lines[index]),
                    fontFamily = CodeFontFamily,
                    color = Color.White
                )
            }
        }

        // Export button in top-right corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            CircleIconButton(
                icon = "\uF019",
                tooltip = "导出日志",
                bgColor = MaterialColor.BLUE_700.color,
                size = 32
            ) {
                scope.launch {
                    val result = exportLogsToZip(state.lines)
                    if (result.isSuccess) {
                        showSuccess = true
                    } else {
                        showError = result.exceptionOrNull()?.message ?: "导出失败"
                    }
                }
            }
        }
    }

    // Success/Error dialogs
    if (showSuccess) {
        alertOk("日志已成功导出")
        showSuccess = false
    }
    showError?.let { error ->
        alertErr("导出失败: $error")
        showError = null
    }
}

private suspend fun exportLogsToZip(lines: List<String>): Result<File> = withContext(Dispatchers.IO) {
    try {
        // Show file chooser dialog
        val chooser = JFileChooser().apply {
            dialogTitle = "选择日志保存位置"
            fileSelectionMode = JFileChooser.FILES_ONLY
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            selectedFile = File(System.getProperty("user.home"), "console_log_$timestamp.zip")
            fileFilter = FileNameExtensionFilter("ZIP 文件 (*.zip)", "zip")
        }

        val result = chooser.showSaveDialog(null)
        if (result != JFileChooser.APPROVE_OPTION) {
            return@withContext Result.failure(Exception("用户取消了操作"))
        }

        var outputFile = chooser.selectedFile
        if (!outputFile.name.endsWith(".zip", ignoreCase = true)) {
            outputFile = File(outputFile.parentFile, "${outputFile.name}.zip")
        }

        // Create zip file
        ZipOutputStream(outputFile.outputStream()).use { zipOut ->
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val entry = ZipEntry("console_log_$timestamp.log")
            zipOut.putNextEntry(entry)
            
            lines.forEach { line ->
                zipOut.write("$line\n".toByteArray(Charsets.UTF_8))
            }
            
            zipOut.closeEntry()
        }

        Result.success(outputFile)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

private val timePrefixRegex = Regex("""^\[\d{2}:\d{2}:\d{2}\]""")
private val goldColor = Color(0xFFFFD700)
private val whiteColor = Color(0xFFFFFFFF)
private val yellowColor = Color(0xFFFFFF00)
private val redColor = Color(0xFFFF3B30)

private fun formatLine(line: String): AnnotatedString {
    val trimmed = line.trimEnd('\r')
    val isTimestamped = timePrefixRegex.containsMatchIn(trimmed)
    return buildAnnotatedString {
        if (isTimestamped) {
            val closeIdx = trimmed.indexOf(']')
            if (closeIdx > 0) {
                val prefix = trimmed.substring(0, closeIdx + 1)
                val rest = trimmed.substring(closeIdx + 1)
                withStyle(SpanStyle(color = goldColor)) { append(prefix) }
                withStyle(SpanStyle(color = pickColor(rest))) { append(rest) }
                return@buildAnnotatedString
            }
        }
        withStyle(SpanStyle(color = pickColor(trimmed))) { append(trimmed) }
    }
}

private fun pickColor(text: String): Color {
    return when {
        text.contains("ERROR", true) ||
            text.contains("SEVERE", true) ||
            text.contains("FATAL", true) ||
            text.contains("Exception", true) -> redColor
        text.contains("WARN", true) ||
            text.contains("WARNING", true) -> yellowColor
        else -> whiteColor
    }
}
