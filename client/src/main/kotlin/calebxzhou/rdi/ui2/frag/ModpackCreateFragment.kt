package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.humanSize
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.component.ImagePicker
import calebxzhou.rdi.ui2.component.ImageSelection
import calebxzhou.rdi.ui2.component.RTextField
import icyllis.modernui.graphics.BitmapFactory

class ModpackCreateFragment: RFragment("制作整合包") {
    private lateinit var nameInput: RTextField
    private lateinit var picker: ImagePicker

    private companion object {
        private const val MAX_FILE_BYTES = 256 * 1024
        private const val MAX_DIMENSION = 512
    }
    override var fragSize: FragmentSize
        get() = FragmentSize.MEDIUM
        set(value) {}
    init {
        contentLayoutInit = {
            nameInput = editText("给你的包起个名字")
            textView("选择一个图标（可选）")
            picker = ImagePicker(fctx).apply {
                validator = ::validateSelection
            }.also { this += it }
        }
        bottomOptionsConfig = {
            "下一步" colored MaterialColor.GREEN_900 with {
                val name = nameInput.text
                val image = picker.selectedBytes
                val params = buildList<Pair<String, Any>> {
                    add("name" to name)
                    image?.let { add("image" to it) }
                }
                RServer.now.hqRequest(true,"modpack/create", params = params){}
            }
        }
    }
    private fun validateSelection(selection: ImageSelection): Boolean {
        val sizeBytes = selection.bytes.size
        if (sizeBytes > MAX_FILE_BYTES) {
            toast("图片大小需小于256KB，当前为${sizeBytes.toLong().humanSize}")
            return false
        }

        val bitmap = BitmapFactory.decodeByteArray(selection.bytes, 0, sizeBytes)
        if (bitmap == null) {
            toast("无法解析所选图片")
            return false
        }

        val width = bitmap.width
        val height = bitmap.height
        bitmap.recycle()

        if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
            toast("图片尺寸需不超过512x512像素，当前为${width}×${height}")
            return false
        }

        return true
    }
}