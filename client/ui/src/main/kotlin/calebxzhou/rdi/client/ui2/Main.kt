package calebxzhou.rdi.client.ui2

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
import calebxzhou.mykotutils.std.decodeBase64
import calebxzhou.mykotutils.std.encodeBase64
import calebxzhou.mykotutils.std.ioScope
import calebxzhou.mykotutils.std.jarResource
import calebxzhou.mykotutils.std.javaExePath
import calebxzhou.rdi.RDI
import calebxzhou.rdi.client.net.loggedAccount
import calebxzhou.rdi.client.service.PlayerService
import calebxzhou.rdi.client.ui.component.alertOk
import calebxzhou.rdi.client.ui2.screen.Ui2Screen
import calebxzhou.rdi.client.ui2.screen.WardrobeScreen
import calebxzhou.rdi.common.json
import calebxzhou.rdi.common.serdesJson
import calebxzhou.rdi.common.util.ioTask
import calebxzhou.rdi.lgr
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.awt.Toolkit
import java.io.File
import java.net.URLClassLoader
import java.nio.charset.StandardCharsets
import kotlin.concurrent.thread
import kotlin.sequences.forEach

lateinit var UIFontFamily: FontFamily
lateinit var ArtFontFamily: FontFamily
lateinit var CodeFontFamily: FontFamily
lateinit var IconFontFamily: FontFamily

fun main() = application {
    val windowIcon = remember {
        jarResource("icon.png").use { stream ->
            BitmapPainter(stream.readAllBytes().decodeToImageBitmap())
        }
    }
    val fontDir = RDI.DIR.resolve("run").resolve("fonts").takeIf { it.exists() }
        ?: RDI.DIR.resolve("fonts")
    val uiFont = fontDir.resolve("1oppo.ttf")
    val artFont = fontDir.resolve("smiley-sans.otf")
    val codeFont = fontDir.resolve("jetbrainsmono.ttf")
    val iconFont = fontDir.resolve("symbolsnerdfont.ttf")

    UIFontFamily = remember {
        FontFamily(listOf(uiFont, iconFont).map { Font(it) })
    }
    CodeFontFamily = remember {
        FontFamily(listOf(codeFont, uiFont, iconFont).map { Font(it) })
    }
    ArtFontFamily = remember {
        FontFamily(listOf(artFont, iconFont).map { Font(it) })
    }
    IconFontFamily = remember {
        FontFamily(listOf(iconFont).map { Font(it) })
    }
    //账号信息
    System.getProperty("rdi.account")?.let {
        loggedAccount = serdesJson.decodeFromString(it.decodeBase64)
    }
    //JWT
    System.getProperty("rdi.jwt")?.let {
        loggedAccount.jwt=it
    }
    if(loggedAccount.jwt == null){
        ioScope.launch {
            val jwt = PlayerService.getJwt(loggedAccount.qq, loggedAccount.pwd)
            loggedAccount.jwt=jwt
        }
    }
    // 设置窗口初始大小为屏幕的2/3，并居中显示
    val screen = remember { Toolkit.getDefaultToolkit().screenSize }
    val windowState = rememberWindowState(
        width = (screen.width * 2 / 3).dp,
        height = (screen.height * 2 / 3).dp,
        position = WindowPosition(Alignment.Center)
    )
    //初始画面
    val initScreen = System.getProperty("rdi.init.screen")?.let {
        Ui2Screen.valueOf(it)
    }
    Window(
        onCloseRequest = ::exitApplication,
        title = System.getProperty("rdi.window.title") ?: "RDI UI2",
        icon = windowIcon,
        state = windowState
    ) {
        MaterialTheme(typography = Typography(defaultFontFamily = UIFontFamily)) {
            when(initScreen){
                Ui2Screen.Wardrobe -> WardrobeScreen()
                else -> App()
            }
            //WardrobeScreen()
        }
    }
}
fun startUi2(initScreen: Ui2Screen){
    alertOk("请稍等几秒，在新窗口中完成操作")
    val classpathEntries = LinkedHashSet<String>()
    val classpathProp = System.getProperty("java.class.path").orEmpty()
    if (classpathProp.isNotBlank()) {
        classpathProp.split(File.pathSeparator)
            .filter { it.isNotBlank() }
            .forEach { classpathEntries += it }
    }
    val cl = Thread.currentThread().contextClassLoader
    (cl as? URLClassLoader)?.urLs?.forEach { url ->
        if (url.protocol == "file") {
            runCatching { File(url.toURI()).absolutePath }
                .getOrNull()
                ?.let { classpathEntries += it }
        }
    }
    val classpath = classpathEntries.joinToString(File.pathSeparator)
    require(classpath.isNotBlank()) { "Empty classpath for UI2 launch" }
    val command = listOf(
        javaExePath,
        "-cp",
        classpath,
        "-Drdi.account=${loggedAccount.json.encodeBase64}",
        "-Drdi.jwt=${loggedAccount.jwt}",
        "-Drdi.init.screen=${initScreen.name}",
        "calebxzhou.rdi.client.ui2.MainKt"
    )
    val process = ProcessBuilder(command)
        .directory(File(".").absoluteFile)
        .redirectErrorStream(true)
        .start()
    val logThread = thread(name = "ui2-log-pump", isDaemon = true) {
        process.inputStream.bufferedReader(StandardCharsets.UTF_8).useLines { lines ->
            lines.forEach { line ->
                if (line.isNotBlank()) {
                    lgr.info { line }
                }
            }
        }
    }
    thread(name = "ui2-process-wait", isDaemon = true) {
        process.waitFor()
        runCatching { process.inputStream.close() }
        runCatching { logThread.join(1000) }
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
