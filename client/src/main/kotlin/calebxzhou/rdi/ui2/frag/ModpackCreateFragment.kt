package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.ui2.FragmentSize
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.component.ImagePicker
import calebxzhou.rdi.ui2.component.RTextField
import calebxzhou.rdi.ui2.editText
import calebxzhou.rdi.ui2.fctx
import calebxzhou.rdi.ui2.plusAssign

class ModpackCreateFragment: RFragment("制作整合包") {
    private lateinit var nameInput: RTextField
    private lateinit var picker: ImagePicker
    override var fragSize: FragmentSize
        get() = FragmentSize.SMALL
        set(value) {}
    init {
        contentLayoutInit = {
            nameInput = editText("给你的包起个名字")
            picker = ImagePicker(fctx).also { this += it }
        }
        bottomOptionsConfig = {
            "下一步" colored MaterialColor.GREEN_900 with {
                val name = nameInput.text


            }
        }
    }
}