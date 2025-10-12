package calebxzhou.rdi

object Const {

    const val MODID = "rdi"
    //是否为调试模式,本地用
    @JvmStatic
    val DEBUG = System.getProperty("rdi.debug").toBoolean()

    //版本号与协议号
    const val VERSION = 0x500
    const val IHQ_VERSION = 0x500
    val SEED = 1145141919810L
    //显示版本
    const val VERSION_NUMBER = "5.1.1"
    //curseforge api(from pcl)
    const val CF_AKEY = $$"$2a$10$bL4bIL5p3A.a1164D5l19u17wM32a106509652d5a.e1a1d59325c"
    @JvmStatic
    val VERSION_DISP = "RDI5skyPro ${if(DEBUG)"DEBUG" else ""} $VERSION_NUMBER"
    const val VERSION_STR = "5.0"

}
