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
    init {
        contentLayoutInit = {
            textView("将使用以下这些Mod：")
            loadMods(this)
        }
        bottomOptionsConfig = {
            "下一步" colored MaterialColor.GREEN_900 with {

            }
        }
    }
    private fun loadMods(contentLayout: LinearLayout) = ioScope.launch {
        ModService.getFingerprintsCurseForge()?.exactMatches?.map { it.id }
            ?.let { ModService.getInfosCurseForge(it) }
            ?.forEach { match ->
                ModService.cfSlugBriefInfo[match.slug]?.let { info ->
                    uiThread {
                        contentLayout += ModCard(
                            contentLayout.context,
                            info.toVo()
                        )
                    }
                }
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