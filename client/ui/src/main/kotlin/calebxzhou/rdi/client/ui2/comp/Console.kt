package calebxzhou.rdi.client.ui2.comp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import calebxzhou.rdi.client.ui2.CodeFontFamily

class ConsoleState(
    private val maxLogLines: Int = 200
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

@Composable
fun Console(
    state: ConsoleState,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val lineCount = state.lines.size

    LaunchedEffect(lineCount) {
        if (lineCount > 0) {
            listState.animateScrollToItem(lineCount - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
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
