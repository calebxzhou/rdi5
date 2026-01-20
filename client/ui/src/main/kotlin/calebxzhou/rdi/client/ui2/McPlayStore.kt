package calebxzhou.rdi.client.ui2

import calebxzhou.rdi.client.ui2.comp.ConsoleState
import calebxzhou.rdi.common.model.McVersion

data class McPlayArgs(
    val title: String,
    val mcVer: McVersion,
    val versionId: String,
    val jvmArgs: List<String>
)

object McPlayStore {
    var current: McPlayArgs? = null
    var process: Process? = null
    val consoleState: ConsoleState = ConsoleState()
}
