package calebxzhou.rdi.ui2.frag.pack

import calebxzhou.rdi.lgr
import calebxzhou.rdi.model.ModBriefInfo
import calebxzhou.rdi.model.ModBriefVo
import calebxzhou.rdi.net.humanSize
import calebxzhou.rdi.service.ModService
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.component.ImageSelection
import calebxzhou.rdi.ui2.component.ModCard
import calebxzhou.rdi.ui2.component.RTextField
import calebxzhou.rdi.ui2.component.alertErr
import calebxzhou.rdi.ui2.frag.RFragment
import calebxzhou.rdi.util.ioScope
import calebxzhou.rdi.util.json
import icyllis.modernui.graphics.BitmapFactory
import icyllis.modernui.widget.LinearLayout
import icyllis.modernui.view.Gravity
import icyllis.modernui.widget.TextView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ModpackCreateFragment: RFragment("制作整合包1") {
    private lateinit var nameInput: RTextField
    //private lateinit var picker: ImagePicker

    private companion object {
        private const val MAX_FILE_BYTES = 256 * 1024
        private const val MAX_DIMENSION = 512
    }
    override var fragSize=FragmentSize.SMALL
    init {
        contentLayoutInit = {
            nameInput = editText("给你的包起个名字")
            /*textView("选择一个图标（可选）")
            picker = ImagePicker(fctx).apply {
                validator = ::validateSelection
            }.also { this += it }*/
        }
        bottomOptionsConfig = {
            "下一步" colored MaterialColor.GREEN_900 with {
               // val image = picker.selectedBytes
                val params = buildList {
                    add("name" to nameInput.text)
                   // add("info" to infoInput.text)
                   // image?.let { add("image" to it.encodeBase64()) }
                }
                ModpackCreate2Fragment(params).go()
            }
        }
    }
    private fun validateSelection(selection: ImageSelection): Boolean {
        val sizeBytes = selection.bytes.size
        if (sizeBytes > MAX_FILE_BYTES) {
            alertErr("图片大小需小于256KB，当前为${sizeBytes.toLong().humanSize}")
            return false
        }

        val bitmap = BitmapFactory.decodeByteArray(selection.bytes, 0, sizeBytes)
        if (bitmap == null) {
            alertErr("无法解析所选图片")
            return false
        }

        val width = bitmap.width
        val height = bitmap.height
        bitmap.recycle()

        if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
            alertErr("图片尺寸需不超过512x512像素，当前为${width}×${height}")
            return false
        }

        return true
    }
}
class ModpackCreate2Fragment(val params: List<Pair<String,String>>): RFragment("制作整合包2") {
    override var fragSize=FragmentSize.LARGE
    private lateinit var modsGrid: LinearLayout
    private lateinit var loadingTv: TextView
    init {
        contentLayoutInit = {
            textView("将使用以下这些Mod：")
            scrollView {
                layoutParams = linearLayoutParam(PARENT, 0) {
                    weight = 1f
                    topMargin = context.dp(12f)
                }
                modsGrid = linearLayout {
                    vertical()
                }
                loadingTv = modsGrid.textView("正在读取已经安装的mod…") {
                    gravity = Gravity.CENTER_HORIZONTAL
                    setTextColor(MaterialColor.GRAY_500.colorValue)
                    setPadding(0, context.dp(8f), 0, context.dp(8f))
                }
            }
            loadMods()
        }
        bottomOptionsConfig = {
            "下一步" colored MaterialColor.GREEN_900 with {

            }
        }
    }
    private fun loadMods() = ioScope.launch {
        val fingerprintData = runCatching { ModService.getFingerprintsCurseForge() }.getOrNull()
        val modIds = fingerprintData?.exactMatches
            ?.map { it.id }
            ?.distinct()
            ?: emptyList()

        if (modIds.isEmpty()) {
            uiThread { showEmptyState("未识别到可匹配的Mod") }
            return@launch
        }
        uiThread {

            loadingTv.text = "识别到${modIds.size}个Mod，正在载入数据…"
        }
        val mods = runCatching { ModService.getInfosCurseForge(modIds) }.getOrElse { err ->
            lgr.warn("加载CurseForge Mod信息失败", err)
            uiThread { showEmptyState("加载CurseForge数据失败") }
            return@launch
        }

        if (mods.isEmpty()) {
            uiThread { showEmptyState("CurseForge未返回相关Mod信息") }
            return@launch
        }

        val seenSlugs = mutableSetOf<String>()
        val cards = mods.mapNotNull { mod ->
            val slug = mod.slug?.lowercase() ?: return@mapNotNull null
            if (!seenSlugs.add(slug)) return@mapNotNull null
            val info = ModService.cfSlugBriefInfo[slug] ?: return@mapNotNull null
            ModCard(modsGrid.context, info.toVo())
        }

        uiThread {
            modsGrid.removeAllViews()
            if (cards.isEmpty()) {
                showEmptyState("暂无可展示的Mod")
                return@uiThread
            }

            cards.chunked(3).forEach { rowItems ->
                val rowContext = modsGrid.context
                val row = LinearLayout(rowContext).apply {
                    horizontal()
                    gravity = Gravity.TOP
                }
                rowItems.forEachIndexed { index, card ->
                    row.addView(card, linearLayoutParam(0, SELF) {
                        weight = 1f
                        if (index < rowItems.lastIndex) {
                            rightMargin = rowContext.dp(12f)
                        }
                    })
                }
                modsGrid.addView(row, linearLayoutParam(PARENT, SELF) {
                    bottomMargin = rowContext.dp(12f)
                })
            }
        }
    }

    private fun showEmptyState(message: String) {
        modsGrid.removeAllViews()
        modsGrid.textView(message) {
            gravity = Gravity.CENTER_HORIZONTAL
            setTextColor(MaterialColor.GRAY_500.colorValue)
            setPadding(0, context.dp(16f), 0, context.dp(16f))
        }
    }

private fun ModBriefInfo.toVo() = ModBriefVo(
    name = name,
    nameCn = nameCn,
    intro = intro,
    iconData = null,
    iconUrls = buildList {
        if (logoUrl.isNotBlank()) add(logoUrl)
    }
)
}