package calebxzhou.rdi

import net.minecraft.core.BlockPos

object Const {
    @JvmField
    val SERVER_PORT = System.getenv("GAME_PORT")?.toIntOrNull()?:System.getProperty("rdi.port")?.toIntOrNull()?:65232

}
