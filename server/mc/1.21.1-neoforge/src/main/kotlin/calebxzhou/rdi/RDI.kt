package calebxzhou.rdi

import com.ibm.icu.text.PluralRules
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import net.neoforged.fml.common.Mod
import org.apache.logging.log4j.LogManager

val lgr = LogManager.getLogger("rdi")
val ioScope: CoroutineScope
    get() = CoroutineScope(Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
        throwable.printStackTrace()
    }
    )
@Mod("rdi")
class RDI {
    companion object{

        val envGameRule = {key: String -> System.getenv("GAME_RULE_${key}")}
        var tickTime1 = 0L
        var tickTime2 = 0L
        var tickDelta = 0L
        @JvmStatic
        val isLagging
            get() = tickDelta>50
    }
    init {
        lgr.info("==========RDI启动中==========")
    }
}