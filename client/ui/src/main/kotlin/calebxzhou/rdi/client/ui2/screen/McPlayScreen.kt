package calebxzhou.rdi.client.ui2.screen

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import calebxzhou.rdi.client.service.GameService
import calebxzhou.rdi.client.ui2.CircleIconButton
import calebxzhou.rdi.client.ui2.MainColumn
import calebxzhou.rdi.client.ui2.MaterialColor
import calebxzhou.rdi.client.ui2.McPlayStore
import calebxzhou.rdi.client.ui2.Space8h
import calebxzhou.rdi.client.ui2.Space8w
import calebxzhou.rdi.client.ui2.TitleRow
import calebxzhou.rdi.client.ui2.comp.Console
import calebxzhou.rdi.common.model.McVersion
import kotlinx.coroutines.launch

/**
 * calebxzhou @ 2026-01-16 20:46
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McPlayScreen(
    title:String,
    mcVer: McVersion,
    versionId: String,
    vararg jvmArgs: String,
    onBack: ()-> Unit )
{
    val scope = rememberCoroutineScope()
    val consoleState = McPlayStore.consoleState
    var process by remember { mutableStateOf(McPlayStore.process?.takeIf { it.isAlive }) }
    val isRunning = process?.isAlive == true
    fun onProcessExit() {
        process = null
        McPlayStore.process = null
    }
    fun startProcess() {
        if (process?.isAlive == true) return
        consoleState.clear()
        val started = GameService.start(mcVer, versionId, *jvmArgs) { line ->
            scope.launch {
                consoleState.append(line)
                if (line.startsWith("启动失败") || line.startsWith("已退出")) {
                    onProcessExit()
                }
            }
        }
        process = started
        McPlayStore.process = started
    }
    LaunchedEffect(versionId, jvmArgs) {
        if (process?.isAlive != true) {
            startProcess()
        }
    }
    MainColumn {
        TitleRow(title,onBack){
            CircleIconButton("\uEAD2","重启MC"){
                process?.destroy()
                startProcess()
            }
            Space8w()
            CircleIconButton("\uF04D","停止MC", bgColor = MaterialColor.ORANGE_900.color){
                process?.destroy()
                onProcessExit()
            }
            Space8w()
            CircleIconButton("\uF05E","强制结束MC", bgColor = MaterialColor.RED_900.color){
                process?.destroyForcibly()
                onProcessExit()
            }
        }
        Space8h()
        Console(consoleState)
    }
}
