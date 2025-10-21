package calebxzhou.rdi

import net.neoforged.fml.common.Mod
import org.apache.logging.log4j.LogManager

val lgr = LogManager.getLogger("rdi")
@Mod("rdi")
class RDI {
    companion object{
        var tickTime1 = 0L
        var tickTime2 = 0L
        var tickDelta = 0L
        @JvmStatic
        val isLagging
            get() = tickDelta>50
    }
    init {
        lgr.info("RDI启动中")
    }
}