package calebxzhou.rdi.ui.frag

import calebxzhou.rdi.common.json
import calebxzhou.rdi.common.model.Host
import calebxzhou.rdi.common.model.Mod
import calebxzhou.rdi.common.model.ModCardVo
import calebxzhou.rdi.common.service.CurseForgeService
import calebxzhou.rdi.common.service.ModService.checkDependencies
import calebxzhou.rdi.common.service.ModService.toVo
import calebxzhou.rdi.common.util.ioTask
import calebxzhou.rdi.net.server
import calebxzhou.rdi.service.ModrinthService
import calebxzhou.rdi.ui.*
import calebxzhou.rdi.ui.component.ModGrid
import calebxzhou.rdi.ui.component.alertErr
import calebxzhou.rdi.ui.component.alertOk
import calebxzhou.rdi.ui.component.confirm
import icyllis.modernui.widget.TextView
import io.ktor.http.*
import org.bson.types.ObjectId
import kotlin.collections.map

class HostModFragment(val hostId: ObjectId) : RFragment("地图的所有Mod") {
    override var fragSize = FragmentSize.FULL
    private lateinit var extraModGrid: ModGrid
    private lateinit var packModGrid: ModGrid
    private lateinit var extraModText: TextView
    private lateinit var packModText: TextView

    init {

        contentViewInit = {
            extraModText = textView("附加Mod：")
            extraModGrid = ModGrid(context, isSelectionEnabled = true).also { this += it }

            packModText = textView("整合包Mod：")
            packModGrid = ModGrid(context).also { this += it }
            server.request<Host>("host/${hostId}"){
                it.data?.let {

                    loadPackMods(it)
                    loadExtraMods(it)
                }?: alertErr("无法加载地图信息")
            }
        }
        titleViewInit = {
            quickOptions {
                "全选" make checkbox with {
                    if (it)
                        extraModGrid.selectAll()
                    else
                        extraModGrid.clearSelection()
                }
                "\uF014 删除选中" colored MaterialColor.RED_900 with {
                    Confirm(
                        false,
                        hostId,
                        extraModGrid.getSelectedMods()
                    ).go()
                }
                "重新下载" colored MaterialColor.BLUE_900 with {
                    confirm("将重新下载这些Mod，并添加到地图。确定吗？"){
                        server.requestU("host/${hostId}/extra_mod",  method = HttpMethod.Put){
                            alertOk("已提交重新下载请求，完成后会发送结果到信箱")
                        }
                    }
                }
                "+ 附加Mod" colored MaterialColor.GREEN_900 with { Add(hostId).go() }
            }
        }
    }

    private fun loadPackMods(host: Host) = ioTask {
        val mods = server.makeRequest<List<Mod>>(path = "modpack/${host.modpackId}/${host.packVer}/mods").data ?: run {
            packModText.text = "整合包Mod：无"
            return@ioTask
        }
        val displayMods = mods.map { original ->
            val vo = when (original.platform) {
                "cf" -> CurseForgeService.slugBriefInfo[original.slug]?.toVo()
                "mr" -> ModrinthService.slugBriefInfo[original.slug]?.toVo()
                else -> null
            } ?: placeholderBrief(original.slug)
            original.copy().also { copy -> copy.vo = vo }
        }
        packModGrid.showMods(displayMods)
    }

    private fun loadExtraMods(host: Host) = ioTask {
        if (host.extraMods.isEmpty()) {
            extraModText.text = "没有附加Mod。"
            return@ioTask
        }
        val displayMods = host.extraMods.map { original ->
            val vo = CurseForgeService.slugBriefInfo[original.slug]?.toVo()
                ?: placeholderBrief(original.slug)
            original.copy().also { copy -> copy.vo = vo }
        }
        extraModGrid.showMods(displayMods)
    }


    private fun placeholderBrief(slug: String) = ModCardVo(
        name = slug,
        nameCn = null,
        intro = "暂无简介",
        iconData = null,
        iconUrls = emptyList()
    )

    class Add(val hostId: ObjectId) : RFragment("向地图添加Mod 请选择") {
        companion object {

        }

        override var fragSize = FragmentSize.FULL
        private lateinit var modGrid: ModGrid

        init {
            contentViewInit = {
                modGrid = ModGrid(context, isSelectionEnabled = true) { updateSelectedCount(it.size) }
                this += modGrid
                modGrid.loadModsFromLocalInstalled()
            }
            titleViewInit = {
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
        }

        private fun updateSelectedCount(count: Int) {
            title = "已选择${count}个Mod"
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

            Confirm(true, hostId, selected).go()

        }
    }

    class Confirm(val add: Boolean, val hostId: ObjectId, val selected: List<Mod>) :
        RFragment("确认${if (add) "添加" else "删除"}这${selected.size}个Mod吗？") {
        override var fragSize = FragmentSize.MEDIUM

        init {
            contentViewInit = {
                val displayMods = selected.map { mod ->
                    mod.copy().also { copy -> copy.vo = mod.vo }
                }
                this += ModGrid(context, mods = displayMods)
            }
            titleViewInit = {
                quickOptions {
                    "☑ 提交" colored MaterialColor.GREEN_900 with { onNext() }
                }
            }
        }

        fun onNext() = ioTask {
            if (add) {
                val etaSecs = selected.size * 10
                server.requestU("host/${hostId}/extra_mod", body = selected.json) {
                        alertOk("已提交Mod添加请求，大约要等${etaSecs / 60}分${etaSecs % 60}秒，完成后会发送结果到信箱")

                }
            } else {
                server.requestU(
                    "host/${hostId}/extra_mod",
                    body = selected.json,
                    method = HttpMethod.Delete
                ) {
                    server.request<Host>("host/${hostId}"){
                        alertOk("成功删除了这${selected.size}个Mod")
                    }

                }
            }
        }
    }


}