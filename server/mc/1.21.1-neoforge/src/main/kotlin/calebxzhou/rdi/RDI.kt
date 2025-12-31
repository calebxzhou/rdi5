package calebxzhou.rdi

import com.ibm.icu.text.PluralRules
import net.neoforged.fml.common.Mod
import org.apache.logging.log4j.LogManager

val lgr = LogManager.getLogger("rdi")

@Mod("rdi")
class RDI {
    companion object{
        @JvmField
        val IHQ_URL = "http://"+(if(Const.DEBUG)"127.0.0.1" else "host.docker.internal")+":65231/"
        @JvmField val ENV = com.mojang.authlib.Environment(RDI.IHQ_URL, RDI.IHQ_URL, "PROD")

        val envDifficulty = System.getenv("DIFFICULTY")?.toIntOrNull() ?: 1
        val envGameMode = System.getenv("GAME_MODE")?.toIntOrNull() ?: 0
        val envLevelType = System.getenv("LEVEL_TYPE") ?: "minecraft:normal"
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