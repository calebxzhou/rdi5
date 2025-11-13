package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.ui2.FragmentSize
import calebxzhou.rdi.ui2.MaterialColor
import calebxzhou.rdi.ui2.frag.pack.ModpackCreateFragment
import calebxzhou.rdi.ui2.go

class ModpackListFragment: RFragment("大家的整合包") {
    override var fragSize = FragmentSize.FULL
    init {
        titleViewInit = {
            quickOptions {
                "⬆️ 上传整合包" colored MaterialColor.BLUE_900 with { ModpackCreateFragment().go() }
            }
        }
    }
}