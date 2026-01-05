package calebxzhou.rdi.ui.frag

import calebxzhou.mykotutils.std.deleteRecursivelyNoSymlink
import calebxzhou.rdi.common.model.Modpack
import calebxzhou.rdi.common.util.ioScope
import calebxzhou.rdi.common.util.ioTask
import calebxzhou.rdi.net.server
import calebxzhou.rdi.service.ModpackService
import calebxzhou.rdi.service.ModpackService.startInstall
import calebxzhou.rdi.ui.FragmentSize
import calebxzhou.rdi.ui.MaterialColor
import calebxzhou.rdi.ui.component.alertErr
import calebxzhou.rdi.ui.component.closeLoading
import calebxzhou.rdi.ui.component.confirm
import calebxzhou.rdi.ui.component.showLoading
import calebxzhou.rdi.ui.horizontal
import calebxzhou.rdi.ui.linearLayout
import calebxzhou.rdi.ui.textView
import calebxzhou.rdi.ui.toast
import calebxzhou.rdi.ui.uiThread
import kotlinx.coroutines.runBlocking

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
                            }
                        }
                    }
                }
                closeLoading()
            }
        }
    }
}