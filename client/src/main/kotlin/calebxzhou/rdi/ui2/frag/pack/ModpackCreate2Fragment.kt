package calebxzhou.rdi.ui2.frag.pack

import calebxzhou.rdi.service.ModService
import calebxzhou.rdi.ui2.FragmentSize
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.PARENT
import calebxzhou.rdi.ui2.component.ModGrid
import calebxzhou.rdi.ui2.dp
import calebxzhou.rdi.ui2.frag.RFragment
import calebxzhou.rdi.ui2.go
import calebxzhou.rdi.ui2.linearLayoutParam
import calebxzhou.rdi.ui2.textView
import calebxzhou.rdi.ui2.uiThread
import calebxzhou.rdi.util.ioScope
import kotlinx.coroutines.launch

/**
 * calebxzhou @ 2025-10-17 13:50
 */

class ModpackCreate2Fragment(val name: String) : RFragment("制作整合包2") {
    override var fragSize = FragmentSize.LARGE
    private lateinit var modGrid: ModGrid

    init {
        contentViewInit = {
            textView("将使用以下这些Mod，请翻到最下面")
            modGrid = ModGrid(context)
            addView(modGrid, linearLayoutParam(PARENT, 0) {
                weight = 1f
                topMargin = context.dp(12f)
            })
            loadMods()
        }
    }

    private fun loadMods() = ioScope.launch {
        val modFiles = ModService().mods
        val modsCount = modFiles.size
        if (modsCount == 0) {
            modGrid.showEmpty("未检测到已安装的mod")
            return@launch
        }

        modGrid.showLoading("找到了${modsCount}个mod，正在从 CurseForge 读取详细信息...大概5~10秒")

        val curseForgeResult = ModService().discoverModsCurseForge()
        val curseForgeMatched = curseForgeResult.matchedFiles
        modGrid.showLoading("CurseForge 已匹配 ${curseForgeMatched.size} 个 Mod，正在载入结果...")
        val briefs = curseForgeResult.cards.map { it.brief }
        val foundCount = briefs.size
        title += "（$foundCount 个Mod）"
        modGrid.showMods(briefs)
        uiThread {
            modGrid.bottomOptions {
                "下一步" colored MaterialColor.GREEN_900 with {
                    ModpackCreate3Fragment(name, curseForgeResult.mods).go()
                }
            }
        }
    }
}