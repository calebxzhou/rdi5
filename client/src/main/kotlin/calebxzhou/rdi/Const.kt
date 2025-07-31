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
    const val VERSION_NUMBER = "5.0"
    @JvmStatic
    val VERSION_DISP = "RDI5 ${if(DEBUG)"DEBUG" else ""} $VERSION_NUMBER"
    const val VERSION_STR = "5.0"

}
