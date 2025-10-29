package calebxzhou.rdi

import net.minecraft.core.BlockPos

object Const {
    val DEBUG = System.getProperty("rdi.debug").toBoolean()
    @JvmField
    val SERVER_PORT = System.getenv("GAME_PORT")?.toIntOrNull()?:System.getProperty("rdi.port")?.toIntOrNull()?:65232
    val TEST_HOST_ID = "68ff1c4c2e060ac942c0581b"
}
