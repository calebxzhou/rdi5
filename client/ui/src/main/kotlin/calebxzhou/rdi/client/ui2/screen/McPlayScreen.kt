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
import calebxzhou.rdi.client.ui2.TitleRow
import calebxzhou.rdi.client.ui2.comp.Console
import calebxzhou.rdi.client.ui2.comp.ConsoleState
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
    var consoleState by remember { mutableStateOf(ConsoleState()) }
    var process by remember { mutableStateOf<Process?>(null) }
    fun startProcess() {
        consoleState.clear()
        process = GameService.start(mcVer, versionId, *jvmArgs) { line ->
            scope.launch { consoleState.append(line) }
        }
    }
    LaunchedEffect(versionId, jvmArgs) {
        startProcess()
    }
    MainColumn {
        TitleRow(title,onBack){
            CircleIconButton("\uEAD2","重启MC"){
                process?.destroy()
                startProcess()
            }
            CircleIconButton("\uF04D","强制结束MC"){
                process?.destroyForcibly()
            }
        }
        Console(consoleState)
    }
}
