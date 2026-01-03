package calebxzhou.rdi.ui.frag

import calebxzhou.rdi.model.McVersion
import calebxzhou.rdi.service.GameService
import calebxzhou.rdi.ui.FragmentSize
import calebxzhou.rdi.ui.button
import calebxzhou.rdi.ui.go
import calebxzhou.rdi.ui.horizontal
import calebxzhou.rdi.ui.linearLayout
import calebxzhou.rdi.ui.textView

class McVersionManageFragment : RFragment("MC版本资源管理") {
    override var fragSize: FragmentSize
        get() = FragmentSize.MEDIUM
        set(value) {}
    init {
        contentViewInit = {
            McVersion.entries.forEach { mcver ->
                linearLayout {
                    textView(mcver.mcVer+"：   ")
                    button("下载全部所需文件") {
                        TaskFragment("下载MC文件") {
                            GameService.downloadVersion(mcver) { log(it) }
                            GameService.downloadLoader(mcver,mcver.firstLoader)
                            { log(it) }
                        }.go()
                    }
                }
                linearLayout {
                    horizontal()
                    quickOptions {
                        "\uF305 仅下载MC核心" with {
                            TaskFragment("下载MC核心文件") {
                                GameService.downloadClient(mcver.metadata) { log(it) }
                            }.go()
                        }
                        "\uDB84\uDE5F 仅下载运行库" with {
                            TaskFragment("下载运行库文件") {
                                GameService.downloadLibraries(mcver.metadata) { log(it) }
                            }.go()
                        }
                        "\uF001 仅下载音频资源" with {
                            TaskFragment("下载音频资源文件") {
                                GameService.downloadAssets(mcver.metadata) { log(it) }
                            }.go()
                        }
                        mcver.loaderVersions.forEach { (loader, version) ->
                            "仅安装${loader.toString().lowercase()}" with {
                                TaskFragment("下载${loader}文件") {
                                    GameService.downloadLoader(mcver, loader) { log(it) }
                                }.go()
                            }
                        }
                    }
                }
            }
        }
    }
}