package calebxzhou.rdi.prox

object Const {
    const val MODID = "rdi"

    //是否为调试模式,本地用
    @JvmStatic
    val DEBUG = System.getProperty("rdi.debug").toBoolean()

    @JvmField
    val SERVER_PORT = System.getProperty("rdi.port")?.toIntOrNull() ?: 65230
}