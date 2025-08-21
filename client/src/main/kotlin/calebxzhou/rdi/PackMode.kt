package calebxzhou.rdi

import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.ui2.frag.alertErr
import calebxzhou.rdi.ui2.frag.alertErrOs
import calebxzhou.rdi.util.mc


val PACK = PackMode.PRO/*PackMode.entries.find { it.verName == mc.launchedVersion }?:run{
    PackMode.entries.find { it.verName==System.getProperty("rdi.pack") }
}?:run {
    alertErrOs(
        """未知的版本 ${mc.launchedVersion}
启动器的版本名称必须为下列之一：
${PackMode.entries.joinToString("\n") { "${it.dscr}: ${it.verName}" }}
修改方法：打开启动器，点版本设置-概览-修改版本名"""
    )
    PackMode.PRO
}*/
enum class PackMode(
    val verName: String,
    val dscr: String,
    val bgImage: String,
    val server: RServer,
) {
    PRO(
        "RDI5SkyPro",
        "专业空岛",
        "1.jpg",
        RServer(
            "rdi.calebxzhou.cn",
            "b5rdi.calebxzhou.cn",
            28531,
            28532,
            28530,
            )
    ),


}