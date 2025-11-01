package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.model.ModBriefVo
import calebxzhou.rdi.service.ModService
import calebxzhou.rdi.ui2.FragmentSize
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.SELF
import calebxzhou.rdi.ui2.button
import calebxzhou.rdi.ui2.component.ModGrid
import calebxzhou.rdi.ui2.component.alertErr
import calebxzhou.rdi.ui2.frameLayoutParam
import calebxzhou.rdi.ui2.go
import calebxzhou.rdi.ui2.linearLayout
import calebxzhou.rdi.ui2.ofView
import calebxzhou.rdi.ui2.paddingDp
import calebxzhou.rdi.ui2.plusAssign
import calebxzhou.rdi.ui2.textView
import calebxzhou.rdi.ui2.uiThread
import calebxzhou.rdi.util.ioTask
import icyllis.modernui.view.Gravity
import icyllis.modernui.widget.TextView
import org.bson.types.ObjectId

class HostTempModFragment(val hostId: ObjectId) : RFragment("添加临时Mod") {
    override var fragSize = FragmentSize.LARGE
    private lateinit var modGrid: ModGrid
    private lateinit var selectedCountText: TextView
    init {
        contentViewInit = {
            linearLayout {
                textView("请选择添加到主机的临时Mod。（更改主机配置会清空）")
                selectedCountText = textView("已选择0个")
                paddingDp(0, 0, 0, 4)
            }
            modGrid = ModGrid(context){updateSelectedCount(it.size) }
            this += modGrid
            loadLocalMods()
        }
        titleViewInit= {
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

    private fun updateSelectedCount(count: Int) {
        selectedCountText.text = "已选择${count}个"
    }

    private fun loadLocalMods() = ioTask {
        val ms = ModService().filterServerOnlyMods()
        if (ms.mods.isEmpty()) {
            alertErr("未找到能添加到主机的Mod，请先安装Mod")
            return@ioTask
        }
        modGrid.showLoading("找到了${ms.mods.size}个mod，正在从CurseForge读取信息...大概5~10秒")
        val curseForgeResult = ms.discoverModsCurseForge()
        modGrid.showLoading("CurseForge已匹配 ${curseForgeResult.matchedFiles.size}个 Mod，正在载入结果...")
        val briefs = curseForgeResult.cards.map { it.brief }
        uiThread {

        }
        modGrid.showMods(briefs)
        updateSelectedCount(modGrid.getSelectedMods().size)


    }

    override fun onNext() {

        val selected = modGrid.getSelectedMods()
        if (selected.isEmpty()) {
            alertErr("请至少选择一个Mod")
            return
        }
        Confirm(hostId,selected).go()

    }

    class Confirm(val hostId: ObjectId,val selected: List<ModBriefVo>) : RFragment("确认添加临时Mod") {
        override var fragSize = FragmentSize.LARGE

        init {
            contentViewInit = {
                textView("确认添加这些Mod？")
                this += ModGrid(context).also {  it.showMods(selected)}
            }
        }

        override fun onNext() {

        }
    }

}