package calebxzhou.rdi

import net.minecraft.core.BlockPos

object Const {
    const val MODID = "rdi"
    //是否为调试模式,本地用
    @JvmStatic
    val DEBUG = System.getProperty("rdi.debug").toBoolean()
    @JvmField
    val SERVER_PORT =  System.getProperty("rdi.port")?.toIntOrNull()?:65232
    const val SEED = 1145141919810L
    //显示版本
    const val VERSION_NUMBER = "5.0"
    val BASE_POS = BlockPos(0,63,0)
    val OLD_PLAYER = "rdi_old_player"
}
