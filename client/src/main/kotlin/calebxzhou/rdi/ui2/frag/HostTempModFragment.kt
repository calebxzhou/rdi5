package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.ModBriefVo
import calebxzhou.rdi.model.pack.Mod
import calebxzhou.rdi.net.server
import calebxzhou.rdi.service.ModService
import calebxzhou.rdi.ui2.FragmentSize
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.component.ModGrid
import calebxzhou.rdi.ui2.component.alertErr
import calebxzhou.rdi.ui2.go
import calebxzhou.rdi.ui2.plusAssign
import calebxzhou.rdi.ui2.textView
import calebxzhou.rdi.util.ioTask
import org.bson.types.ObjectId

class HostTempModFragment(val hostId: ObjectId) : RFragment("向主机添加临时Mod 请选择") {
    override var fragSize = FragmentSize.LARGE
    private lateinit var modGrid: ModGrid
    var   curseForgeResult: ModService.CurseForgeLocalResult?=null
    init {
        contentViewInit = {
            modGrid = ModGrid(context, isSelectionEnabled = true) { updateSelectedCount(it.size) }
            this += modGrid
            loadLocalMods()
        }
        titleViewInit= {
            textView("更新整合包会清空临时mod。")
            quickOptions {
                "\uD83D\uDDD1\uFE0F 全不选" colored MaterialColor.RED_900 with {
                    modGrid.clearSelection()
                }
                "☑ 全选" colored MaterialColor.BLUE_900 with {
                    modGrid.selectAll()
                }
                "➡️ 下一步" colored MaterialColor.GREEN_900 with { onNext() }
            }
        }
    }

    private fun updateSelectedCount(count: Int)  {
        title = "已选择${count}个Mod"
    }

    private fun loadLocalMods() = ioTask {
        val ms = ModService().filterServerOnlyMods()
        if (ms.mods.isEmpty()) {
            alertErr("未找到能添加到主机的Mod，请先安装Mod")
            return@ioTask
        }
        modGrid.showLoading("找到了${ms.mods.size}个mod，正在从CurseForge读取信息...大概5~10秒")
        val curseForgeResult = ms.discoverModsCurseForge()
        this.curseForgeResult=curseForgeResult
        modGrid.showLoading("CurseForge已匹配 ${curseForgeResult.matchedFiles.size}个 Mod，正在载入结果...")
        val briefs = curseForgeResult.cards.map { it.brief }

        modGrid.showMods(briefs)
        updateSelectedCount(modGrid.getSelectedMods().size)


    }

    fun onNext() {

        val selected = modGrid.getSelectedMods()
        if (selected.isEmpty()) {
            alertErr("请至少选择一个Mod")
            return
        }
        curseForgeResult?.let {
            Confirm(hostId,it.mods,it.cards.map { it.brief }).go()
        }

    }

    class Confirm(val hostId: ObjectId, val selected: List<Mod>, val selectedVo: List<ModBriefVo>) : RFragment("确认添加这${selected.size}个Mod吗？") {
        override var fragSize = FragmentSize.LARGE

        init {
            contentViewInit = {
                this += ModGrid(context).also {  it.showMods(selectedVo) }
            }
            titleViewInit = {
                quickOptions {
                    "☑ 提交" colored MaterialColor.GREEN_900 with { onNext() }
                }
            }
        }

        fun onNext()=ioTask {
            server.requestU("host/tempmod"){}
        }
    }

}