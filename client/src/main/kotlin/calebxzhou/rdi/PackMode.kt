package calebxzhou.rdi

import calebxzhou.rdi.model.RServer
import calebxzhou.rdi.ui2.frag.alertErr
import calebxzhou.rdi.ui2.frag.alertErrOs
import calebxzhou.rdi.util.mc


val PACK = PackMode.entries.find { it.verName == mc.launchedVersion }?:run{
    PackMode.entries.find { it.verName==System.getProperty("rdi.pack") }
}?:run {
    alertErrOs(
        """未知的版本 ${mc.launchedVersion}
启动器的版本名称必须为下列之一：
${PackMode.entries.joinToString("\n") { "${it.dscr}: ${it.verName}" }}
修改方法：打开启动器，点版本设置-概览-修改版本名"""
    )
    PackMode.SEA
}
enum class PackMode(
    val verName: String,
    val dscr: String,
    val bgImage: String,
    val server: RServer,
) {
    SEA(
        "RDI5sea",
        "海岛",
        "1.jpg",
        RServer(
            "rdi.calebxzhou.cn",
            "b5rdi.calebxzhou.cn",
            28511,
            listOf(28510,58210)
            )
    ),

    MECH(
        "RDI5mech",
        "机动大陆",
        "2.jpg",
        RServer(
            "rdi.calebxzhou.cn",
            "b5rdi.calebxzhou.cn",
            28521,
            listOf(28520,58220)
        )
    )
}