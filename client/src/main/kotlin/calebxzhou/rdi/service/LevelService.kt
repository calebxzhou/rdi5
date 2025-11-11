package calebxzhou.rdi.service

import calebxzhou.rdi.Const
import calebxzhou.rdi.model.RAccount
import calebxzhou.rdi.ui2.frag.ProfileFragment
import calebxzhou.rdi.ui2.frag.TitleFragment
import calebxzhou.rdi.ui2.goto
import calebxzhou.rdi.ui2.mcScreen
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.renderThread
import calebxzhou.rdi.util.set
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.world.Difficulty
import net.minecraft.world.level.GameRules
import net.minecraft.world.level.GameType
import net.minecraft.world.level.LevelSettings
import net.minecraft.world.level.WorldDataConfiguration
import net.minecraft.world.level.levelgen.WorldOptions
import net.minecraft.world.level.levelgen.presets.WorldPresets

object LevelService {

  /*  fun startLevel() {
        val levelName = "rdi" + RAccount.now?.name
        if (mc.levelSource.levelExists(levelName)) {
            mc.createWorldOpenFlows().openWorld(levelName) {
                goto(TitleFragment())
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
                WorldOptions(Const.SEED, true, true),
                WorldPresets::createNormalWorldDimensions,
                ProfileFragment().mcScreen
            )
        }
    }
*/
    fun openFlatLevel()  {
        mc set TitleScreen()
        val levelName = "rdi_creative"
        if (mc.levelSource.levelExists(levelName)) {
            renderThread {
                mc.createWorldOpenFlows().openWorld(levelName) {
                    mc set TitleScreen()
                }
            }
        } else {
            val levelSettings = LevelSettings(
                levelName,
                GameType.CREATIVE,
                false,
                Difficulty.PEACEFUL,
                true,
                GameRules().apply {
                    getRule(GameRules.RULE_DAYLIGHT).set(false, null)
                    getRule(GameRules.RULE_WEATHER_CYCLE).set(false, null)
                    getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null)
                },
                WorldDataConfiguration.DEFAULT
            )
            renderThread {
                mc.createWorldOpenFlows().createFreshLevel(
                    levelName,
                    levelSettings,
                    WorldOptions(Const.SEED, true, true),
                    WorldPresets::createNormalWorldDimensions,
                    TitleScreen()
                )
            }
        }
    }
}