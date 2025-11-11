package calebxzhou.rdi.ui2.frag.pack

import calebxzhou.rdi.service.ModService
import calebxzhou.rdi.service.checkDependencies
import calebxzhou.rdi.ui2.FragmentSize
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.PARENT
import calebxzhou.rdi.ui2.component.ModGrid
import calebxzhou.rdi.ui2.component.alertErr
import calebxzhou.rdi.ui2.dp
import calebxzhou.rdi.ui2.frag.HostModFragment.Confirm
import calebxzhou.rdi.ui2.frag.RFragment
import calebxzhou.rdi.ui2.go
import calebxzhou.rdi.ui2.linearLayoutParam
import calebxzhou.rdi.ui2.plusAssign
import calebxzhou.rdi.ui2.textView
import calebxzhou.rdi.ui2.uiThread
import calebxzhou.rdi.util.ioScope
import kotlinx.coroutines.launch

/**
 * calebxzhou @ 2025-10-17 13:50
 */

class ModpackCreate2Fragment() : RFragment("制作整合包") {
    override var fragSize = FragmentSize.LARGE
    private lateinit var modGrid: ModGrid

    init {
        titleViewInit = {
            textView("选择你要使用的Mod。")
            quickOptions {
                "全选" make checkbox with {
                    if (it)
                        modGrid.selectAll()
                    else
                        modGrid.clearSelection()
                }
                "➡️ 下一步" colored MaterialColor.GREEN_900 with { onNext() }
            }
        }
        contentViewInit = {

            modGrid = ModGrid(context, isSelectionEnabled = true)
            this += modGrid
            modGrid.loadModsFromLocalInstalled()
        }
    }
    fun onNext() {

        val selected = modGrid.getSelectedMods()
        if (selected.isEmpty()) {
            alertErr("请至少选择一个Mod")
            return
        }
        if (modGrid.modLoadResult == null) {
            alertErr("CurseForge 信息尚未加载完成，请稍后再试")
            return
        }

        val missingDeps = modGrid.getSelectedMods()
            .mapNotNull { it.file }
            .checkDependencies()

        if (missingDeps.isNotEmpty()) {
            val detail = missingDeps.joinToString("\n") { unmatched ->
                val deps = unmatched.missing.joinToString(", ") { missing ->
                    missing.version?.let { ver -> "${missing.modId} ($ver)" } ?: missing.modId
                }
                "${unmatched.modId}: $deps"
            }
            alertErr("以下 Mod 缺少前置:\n$detail")
            return
        }



    }
}