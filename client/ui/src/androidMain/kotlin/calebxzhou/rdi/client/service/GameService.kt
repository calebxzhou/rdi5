package calebxzhou.rdi.client.service

import calebxzhou.rdi.common.model.TaskContext
import calebxzhou.rdi.common.model.TaskProgress

/**
 * Android actual for installer bootstrapper expect functions.
 * On Android, the installer bootstrapper is not run — FCL handles this.
 */
internal actual fun GameService.runInstallerBootstrapperDesktop(
    holder: GameService.LoaderInstallHolder,
    ctx: TaskContext
) {
    ctx.emitProgress(TaskProgress("Android不运行客户端安装器 (由FCL处理)", 1f))
}

internal actual fun GameService.runServerInstallerBootstrapperDesktop(
    holder: GameService.LoaderInstallHolder,
    ctx: TaskContext
) {
    ctx.emitProgress(TaskProgress("Android不运行服务端安装器", 1f))
}
