package calebxzhou.rdi

import calebxzhou.rdi.service.LevelService
import com.aayushatharva.brotli4j.Brotli4jLoader
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod
import net.neoforged.neoforge.internal.versions.neoforge.NeoForgeVersion.MOD_ID
import org.apache.logging.log4j.LogManager
import java.io.File

val lgr = LogManager.getLogger("rdi")
var TOTAL_TICK_DELTA = 0f
@Mod("rdi")
object RDI {

    init {
        lgr.info("RDI启动中")
        Brotli4jLoader.ensureAvailability()
        LevelService
        File("rdi").mkdir()
    }
}