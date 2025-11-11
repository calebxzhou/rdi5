package calebxzhou.rdi.ui2.frag.pack

import calebxzhou.rdi.net.humanSize
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.ui2.component.ImageSelection
import calebxzhou.rdi.ui2.component.RTextField
import calebxzhou.rdi.ui2.component.alertErr
import calebxzhou.rdi.ui2.frag.RFragment
import icyllis.modernui.core.Context
import icyllis.modernui.graphics.BitmapFactory
import icyllis.modernui.graphics.drawable.ColorDrawable
import icyllis.modernui.view.Gravity
import icyllis.modernui.view.View
import icyllis.modernui.widget.LinearLayout

class ModpackCreateFragment : RFragment("制作整合包") {

    //private lateinit var picker: ImagePicker

    private companion object {
        private const val MAX_FILE_BYTES = 256 * 1024
        private const val MAX_DIMENSION = 512
    }

    override var fragSize = FragmentSize.FULL

    init {
        contentViewInit = {

        }
        titleViewInit = {
            linearLayout {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = linearLayoutParam(PARENT, SELF)
                gravity = Gravity.CENTER_VERTICAL


                listOf(
                    "\uDB83\uDCA0 Mod", // if not selected 󰲡
                    "\uDB83\uDCA2 配置 ", // if not selected 󰲣
                    "\uDB83\uDCA4 魔改"  // if not selected 󰲥
                ).forEach { label ->
                    textView(label) {
                        layoutParams = linearLayoutParam(dp(120f), SELF) {
                            weight = 1f
                            marginEnd=10
                        }
                        background = ColorDrawable(MaterialColor.TEAL_900.colorValue)
                        gravity = Gravity.CENTER
                        paddingDp(4)
                    }
                }
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
    class Step1(ctx: Context): View(ctx){
        init {


        }
    }
}
