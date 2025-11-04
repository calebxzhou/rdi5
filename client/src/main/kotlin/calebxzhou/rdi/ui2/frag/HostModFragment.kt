package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.Const
import calebxzhou.rdi.model.ModBriefVo
import calebxzhou.rdi.model.pack.Mod
import calebxzhou.rdi.net.server
import calebxzhou.rdi.service.ModService
import calebxzhou.rdi.ui2.FragmentSize
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.component.ModGrid
import calebxzhou.rdi.ui2.component.alertErr
import calebxzhou.rdi.ui2.component.alertOk
import calebxzhou.rdi.ui2.go
import calebxzhou.rdi.ui2.plusAssign
import calebxzhou.rdi.ui2.textView
import calebxzhou.rdi.util.ioTask
import calebxzhou.rdi.util.json
import calebxzhou.rdi.util.toFixed
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import org.bson.types.ObjectId
import java.io.File

class HostModFragment(val hostId: ObjectId) : RFragment("向主机添加Mod 请选择") {
    companion object{
        val MOCK_DATA: List<ModBriefVo> =  File("temp_mods.cbor").readBytes().let { Cbor.decodeFromByteArray(it) }

    }
    override var fragSize = FragmentSize.FULL
    private lateinit var modGrid: ModGrid
    var curseForgeResult: ModService.CurseForgeLocalResult?=null
    init {
        contentViewInit = {
            modGrid = ModGrid(context, isSelectionEnabled = true) { updateSelectedCount(it.size) }
            this += modGrid
            loadLocalMods()
        }
        titleViewInit= {
            quickOptions {
                "全选" make checkbox with {
                    if(it)
                        modGrid.selectAll()
                    else
                        modGrid.clearSelection()
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
        val briefs = if(!Const.USE_MOCK_DATA){
            val curseForgeResult = ms.discoverModsCurseForge()
            this.curseForgeResult=curseForgeResult
            modGrid.showLoading("CurseForge已匹配 ${curseForgeResult.matchedFiles.size}个 Mod，正在载入结果...")
             curseForgeResult.cards.map { it.brief }
        } else MOCK_DATA
       // if (Const.DEBUG) File("temp_mods.cbor").writeBytes(briefs.cbor)
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
            val etaSecs = selected.size * 10
            server.requestU("host/${hostId}/extra_mod", body = selected.json){
                alertOk("已提交Mod添加请求，大约要等${etaSecs/60}分${etaSecs%60}秒，成功会给你发信")
            }
        }
    }

}