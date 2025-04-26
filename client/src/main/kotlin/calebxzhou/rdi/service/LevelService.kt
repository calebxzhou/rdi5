package calebxzhou.rdi.service

import calebxzhou.rdi.Const
import calebxzhou.rdi.mixin.ARegionFileVersion
import calebxzhou.rdi.util.mc
import com.aayushatharva.brotli4j.decoder.BrotliInputStream
import com.aayushatharva.brotli4j.encoder.BrotliOutputStream
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.util.FastBufferedInputStream
import net.minecraft.world.Difficulty
import net.minecraft.world.level.GameRules
import net.minecraft.world.level.GameType
import net.minecraft.world.level.LevelSettings
import net.minecraft.world.level.WorldDataConfiguration
import net.minecraft.world.level.chunk.storage.RegionFileVersion
import net.minecraft.world.level.levelgen.WorldOptions
import net.minecraft.world.level.levelgen.presets.WorldPresets
import net.minecraft.world.level.levelgen.presets.WorldPresets.createNormalWorldDimensions
import java.io.BufferedOutputStream

object LevelService {
    @JvmField
    val BR_STORAGE_VERSION = ARegionFileVersion.invokeRegister(RegionFileVersion(
        64,
        "br",
        {s -> FastBufferedInputStream(BrotliInputStream(s))},
        {s -> BufferedOutputStream(BrotliOutputStream(s))},
    ))
    @JvmStatic
    fun start(){
        startLevel()
    }
    private fun startLevel() {
        val levelName = "rdi"
        if (mc.levelSource.levelExists(levelName)) {
            mc.createWorldOpenFlows().openWorld(levelName){}
        } else {
            val levelSettings = LevelSettings(
                levelName,
                GameType.SURVIVAL,
                false,
                Difficulty.HARD,
                true,
                GameRules().apply {
                    getRule(GameRules.RULE_KEEPINVENTORY).set(true, null)
                },
                WorldDataConfiguration.DEFAULT
            )
            mc.createWorldOpenFlows().createFreshLevel(
                levelName,
                levelSettings,
                WorldOptions(Const.SEED, true, true),WorldPresets::createNormalWorldDimensions, TitleScreen()
            )
        }
    }
}