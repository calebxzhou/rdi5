package calebxzhou.rdi

import calebxzhou.rdi.service.LevelService
import calebxzhou.rdi.util.json
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.neoforged.fml.common.Mod
import org.apache.logging.log4j.LogManager
import java.io.File

val lgr = LogManager.getLogger("rdi")
var TOTAL_TICK_DELTA = 0f
@Mod("rdi")
class RDI {

    init {
        lgr.info("RDI启动中")
        LevelService
        File("rdi").mkdir()

    }
}