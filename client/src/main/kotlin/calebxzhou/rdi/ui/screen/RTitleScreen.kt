package calebxzhou.rdi.ui.screen

import calebxzhou.rdi.Const
import calebxzhou.rdi.net.RServer
import calebxzhou.rdi.net.RServer.Companion.OFFICIAL_DEBUG
import calebxzhou.rdi.net.RServer.Companion.OFFICIAL_NNG
import calebxzhou.rdi.ui.UiHeight
import calebxzhou.rdi.ui.UiWidth
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.pressingKey
import calebxzhou.rdi.util.rdiAsset
import com.mojang.blaze3d.platform.InputConstants
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen
import net.minecraft.core.registries.Registries
import net.minecraft.world.Difficulty
import net.minecraft.world.level.GameRules
import net.minecraft.world.level.GameType
import net.minecraft.world.level.LevelSettings
import net.minecraft.world.level.WorldDataConfiguration
import net.minecraft.world.level.levelgen.WorldOptions
import net.minecraft.world.level.levelgen.presets.WorldPresets

class RTitleScreen : RScreen("主页") {
    override var showTitle = false
    override var closeable = false
    val LOGO = rdiAsset("textures/logo.png")
    var shiftMode = false
    var ctrlMode = false


    public override fun init() {
        RServer.now=null
        mc.level?.let {
            it.disconnect()
            mc.disconnect()
        }





        super.init()
    }

    fun startMulti() {

        if (Const.DEBUG) {
            OFFICIAL_DEBUG.connect()
        } else {
            OFFICIAL_NNG.connect()
        }
    }


    override fun shouldCloseOnEsc(): Boolean {
        return false
    }
    override fun doRender(gg: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        //panorama.render(partialTick, Mth.clamp(1f, 0.0f, 1.0f))
        RenderSystem.enableBlend()
        gg.setColor(1.0f, 1.0f, 1.0f, 1.0f)
        gg.fill(0,UiHeight/4,UiWidth,UiHeight/4+64,0xaa000000.toInt())
        gg.blit(LOGO, UiWidth / 2 - 60, UiHeight / 4, -0.0625f, 0.0f, 128, 64, 128, 64)
         gg.setColor(1.0f, 1.0f, 1.0f, 1.0f)


    }
    var counter = 0
    override fun doTick() {


        shiftMode = mc pressingKey InputConstants.KEY_LSHIFT
        ctrlMode = mc pressingKey InputConstants.KEY_LCONTROL
        counter++


        if (mc pressingKey InputConstants.KEY_0) {
            mc go SelectWorldScreen(this)
        }
        if (mc pressingKey InputConstants.KEY_1) {
            openFlatLevel()
        }
    }

    override fun onPressEnterKey() {
        startMulti()
    }
    private fun openFlatLevel() {
        val levelName = "rdi_creative"
        if (mc.levelSource.levelExists(levelName)) {
            mc.createWorldOpenFlows().openWorld(levelName){}
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
            mc.createWorldOpenFlows().createFreshLevel(
                levelName,
                levelSettings,
                WorldOptions(0, false, false),
                {
                    it.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                        .value().createWorldDimensions();
                },
                this
            )
        }
    }


}