package calebxzhou.rdi.ui2.frag.pack

import calebxzhou.rdi.model.pack.Mod
import calebxzhou.rdi.ui2.FragmentSize
import calebxzhou.rdi.ui2.frag.RFragment
import calebxzhou.rdi.ui2.textView

class ModpackCreate4Fragment(name: String, mods: List<Mod>, conf: Map<String, ByteArray>, kjs: Map<String, ByteArray>) : RFragment("制作整合包4 确认信息") {
    override var fragSize: FragmentSize
        get() = FragmentSize.SMALL
        set(value) {}
    init {
        contentLayoutInit = {
            textView("整合包名称：$name")
            textView("包含Mod数量：${mods.size} 个")
            textView("配置文件数量：${conf.size} 个")
            textView("KubeJS脚本数量：${kjs.size} 个")
        }
        bottomOptionsConfig = {
            "完成" colored calebxzhou.rdi.ui2.MaterialColor.GREEN_900 with {
                //todo
            }
        }
    }
}