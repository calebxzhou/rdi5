package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.ui2.textView

class HostTempModFragment: RFragment("主机的临时Mod") {
    init {
        contentLayoutInit = {
            textView("临时mod只能作为测试用途。一旦更改了主机的配置（更新整合包/更换存档...），所有的临时mod会被清空。要永久保留，请制作整合包。")
        }
    }
}