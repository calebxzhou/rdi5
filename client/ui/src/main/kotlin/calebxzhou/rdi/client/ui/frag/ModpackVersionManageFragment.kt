package calebxzhou.rdi.client.ui.frag

import calebxzhou.mykotutils.std.deleteRecursivelyNoSymlink
import calebxzhou.rdi.common.model.Modpack
import calebxzhou.rdi.common.util.ioTask
import calebxzhou.rdi.client.net.server
import calebxzhou.rdi.client.service.ModpackService
import calebxzhou.rdi.client.service.ModpackService.startInstall
import calebxzhou.rdi.client.ui.*
import calebxzhou.rdi.client.ui.component.alertErr
import calebxzhou.rdi.client.ui.component.closeLoading
import calebxzhou.rdi.client.ui.component.confirm
import calebxzhou.rdi.client.ui.component.showLoading
import java.awt.Desktop

class ModpackVersionManageFragment : RFragment("本地整合包版本管理") {
    override var fragSize = FragmentSize.MEDIUM

    init {
        contentViewInit = {
            showLoading()
            ioTask {
                val dirs = ModpackService.getLocalPackDirs()
                if(dirs.isEmpty()){
                    textView("尚未安装本地整合包")
                    closeLoading()
                    return@ioTask
                }
                dirs.forEach { packdir ->
                    uiThread {
                        linearLayout {
                            horizontal()
                            textView("${packdir.modpackName} ${packdir.verName}")
                            quickOptions {
                                "删除" colored MaterialColor.RED_900 with {
                                    confirm("确定要删除本地整合包版本 ${packdir.modpackName} ${packdir.verName} 吗？") {
                                        packdir.dir.deleteRecursivelyNoSymlink()
                                        toast("已删除")
                                        reloadFragment()
                                    }
                                }
                                "重装" colored MaterialColor.PINK_900 with {
                                    server.request<Modpack>("modpack/${packdir.modpackId}") { resp ->
                                        resp.data?.let { modpack ->
                                            modpack.versions.find { it.name == packdir.verName }
                                                ?.startInstall(modpack.mcVer, modpack.modloader, modpack.name)
                                                ?: alertErr("未找到对应版本信息，可能该版本已被删除，无法重新下载")
                                        } ?: alertErr("未找到对应整合包信息，可能该整合包已被删除，无法重新下载")
                                    }
                                }
                                "打开文件夹" with {
                                    val dir = packdir.dir
                                    if (!dir.exists()) {
                                        alertErr("目录不存在: ${dir.absolutePath}")
                                        return@with
                                    }
                                    runCatching {
                                        if (Desktop.isDesktopSupported()) {
                                            Desktop.getDesktop().open(dir)
                                        } else {
                                            ProcessBuilder("explorer", dir.absolutePath).start()
                                        }
                                    }.onFailure {
                                        alertErr("无法打开目录: ${it.message}")
                                    }
                                }
                            }
                        }
                    }
                }
                closeLoading()
            }
        }
    }
}
