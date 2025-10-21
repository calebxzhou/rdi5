package calebxzhou.rdi

import net.minecraft.core.BlockPos

object Const {
    @JvmField
    val SERVER_PORT =  System.getProperty("rdi.port")?.toIntOrNull()?:65232
}
