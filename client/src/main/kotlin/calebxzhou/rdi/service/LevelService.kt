package calebxzhou.rdi.service

import calebxzhou.rdi.Const
import calebxzhou.rdi.ui2.frag.ProfileFragment
import calebxzhou.rdi.ui2.frag.TitleFragment
import calebxzhou.rdi.ui2.mcScreen
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.mc
import net.minecraft.world.Difficulty
import net.minecraft.world.level.GameRules
import net.minecraft.world.level.GameType
import net.minecraft.world.level.LevelSettings
import net.minecraft.world.level.WorldDataConfiguration
import net.minecraft.world.level.levelgen.WorldOptions
import net.minecraft.world.level.levelgen.presets.WorldPresets

object LevelService {

    fun startLevel() {
        val levelName = "rdi"
        if (mc.levelSource.levelExists(levelName)) {
            mc.createWorldOpenFlows().openWorld(levelName){
                mc go TitleFragment()
            }
        } else {
            val levelSettings = LevelSettings(
                levelName,
                GameType.SURVIVAL,
                false,
                Difficulty.HARD,
                Const.DEBUG,
                GameRules().apply {
                    getRule(GameRules.RULE_KEEPINVENTORY).set(true, null)
                },
                WorldDataConfiguration.DEFAULT
            )
            mc.createWorldOpenFlows().createFreshLevel(
                levelName,
                levelSettings,
                WorldOptions(Const.SEED, true, true),WorldPresets::createNormalWorldDimensions, ProfileFragment().mcScreen
            )
        }
    }
}