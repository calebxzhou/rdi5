package calebxzhou.rdi.client

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.Typography
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import calebxzhou.mykotutils.std.jarResource
import java.awt.Toolkit
import java.io.File

lateinit var DefaultFontFamily: FontFamily
fun main() = application {

    val windowIcon = remember {
        jarResource("icon.png").use { stream ->
            BitmapPainter(stream.readAllBytes().decodeToImageBitmap())
        }
    }
    DefaultFontFamily = remember {
        File("fonts/1oppo.ttf").takeIf { it.exists() } ?.let { FontFamily(Font(it))} ?: FontFamily.Default
    }
    // 设置窗口初始大小为屏幕的2/3，并居中显示
    val screen = remember { Toolkit.getDefaultToolkit().screenSize }
    val windowState = rememberWindowState(
        width = (screen.width * 2 / 3).dp,
        height = (screen.height * 2 / 3).dp,
        position = WindowPosition(Alignment.Center)
    )
    Window(
        onCloseRequest = ::exitApplication,
        title = System.getProperty("rdi.window.title") ?: "RDI UI2",
        icon = windowIcon,
        state = windowState
    ) {
        MaterialTheme(typography = Typography(defaultFontFamily = DefaultFontFamily)) {
            App()
        }
    }
}

@Composable
fun App() {
    var count by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Hello, Compose for Desktop!", style = MaterialTheme.typography.h3)
        Button(onClick = { count++ }) {
            Text("Click me: $count")
        }
    }
}
