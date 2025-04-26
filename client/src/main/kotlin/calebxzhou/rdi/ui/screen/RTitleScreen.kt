package calebxzhou.rdi.ui.screen

import calebxzhou.rdi.Const
import calebxzhou.rdi.model.RServer
import calebxzhou.rdi.model.RServer.Companion.OFFICIAL_DEBUG
import calebxzhou.rdi.model.RServer.Companion.OFFICIAL_NNG
import calebxzhou.rdi.ui.component.RScreen
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.mc.UiHeight
import calebxzhou.rdi.util.mc.UiWidth
import calebxzhou.rdi.util.pressingKey
import calebxzhou.rdi.util.rdiAsset
import com.mojang.blaze3d.platform.InputConstants
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundSource
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
    val LOGO: ResourceLocation =
        ResourceLocation("rdi", "textures/logo.png")
    override var dimBg = false
    val ENTER_KEY_TEXTURE: ResourceLocation = rdiAsset("textures/gui/enter_key.png")
    var shiftMode = false
    var ctrlMode = false

    companion object {

    }


    public override fun init() {
        RServer.now=null
        mc.level?.let {
            it.disconnect()
            mc.clearLevel()
        }

        //关闭音乐
        mc.options.getSoundSourceOptionInstance(SoundSource.MUSIC).set(0.0)
        mc.options.getSoundSourceOptionInstance(SoundSource.AMBIENT).set(0.0)
        mc.options.directionalAudio().set(true)
        mc.options.save()




        super.init()
    }

    fun startMulti() {

        if (Const.DEBUG) {
            OFFICIAL_DEBUG.ping()
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
        if(counter%20>8)
        gg.blit(ENTER_KEY_TEXTURE, UiWidth / 2 - 45, UiHeight - 80, -0.0625f, 0.0f, 100, 60, 100,60)
        gg.setColor(1.0f, 1.0f, 1.0f, 1.0f)


    }
    var counter = 0
    override fun doTick() {


        shiftMode = mc pressingKey InputConstants.KEY_LSHIFT
        ctrlMode = mc pressingKey InputConstants.KEY_LCONTROL
        counter++
        /*if (mc pressingKey InputConstants.KEY_Z) {
            mc goScreen object : RScreen("文档测试") {
                override fun init() {

                    val widget = docWidget {
                        h1("一级标题一级标题一级标题")
                        h2("二级标题二级标题二级标题")
                        p("正文啊啊啊啊啊啊啊啊啊啊正文啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶反反复复烦烦烦烦烦烦烦烦烦烦烦烦烦烦烦啊啊啊啊啊啊啊啊啊顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶反反复复烦烦烦烦烦烦烦烦烦烦烦烦烦烦烦")
                        spacer(5)
                        img("gui/title/mojangstudios")
                        items(
                            ItemStack(Items.ENCHANTED_BOOK, 16),
                            ItemStack(Items.DARK_OAK_DOOR, 16),
                            ItemStack(Items.GRASS_BLOCK, 16),
                            ItemStack(Items.GRASS_BLOCK, 16),
                            ItemStack(Items.GRASS_BLOCK, 16),
                        )
                        p("正文正文啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶反反复复烦烦烦烦烦烦烦烦烦烦烦烦烦烦烦正文啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶反反复复烦烦烦烦烦烦烦烦烦烦烦烦烦烦烦啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊顶顶顶顶顶顶正文啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊正文啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶反反复复烦烦烦烦烦烦烦烦烦烦烦烦烦烦烦正文啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶反反复复烦烦烦烦烦烦烦烦烦烦烦烦烦烦烦啊啊啊啊顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶反反复复烦烦烦烦烦烦烦烦烦烦烦烦烦烦烦顶顶顶顶顶顶顶顶顶反反复复烦烦烦烦烦烦烦烦烦烦烦烦烦烦烦")
                        p("正文啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶反反复复烦烦烦烦烦烦烦烦烦烦烦烦烦烦烦正文啊啊啊啊正文啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶反反复复烦烦烦烦烦烦烦烦烦烦烦烦烦烦烦正文啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶反反复复烦烦烦烦烦烦烦烦烦烦烦烦烦烦烦啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶反反复复烦烦烦烦烦烦烦烦烦烦烦烦烦烦烦正文啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶反反复复烦烦烦烦烦烦烦烦烦烦烦烦烦烦烦正文啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶反反复复烦烦烦烦烦烦烦烦烦烦烦烦烦烦烦正文啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶反反复复烦烦烦烦烦烦烦烦烦烦烦烦烦烦烦正文啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶反反复复烦烦烦烦烦烦烦烦烦烦烦烦烦烦烦正文啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶反反复复烦烦烦烦烦烦烦烦烦烦烦烦烦烦烦正文啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶反反复复烦烦烦烦烦烦烦烦烦烦烦烦烦烦烦正文啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶反反复复烦烦烦烦烦烦烦烦烦烦烦烦烦烦烦正文啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶反反复复烦烦烦烦烦烦烦烦烦烦烦烦烦烦烦正文啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶反反复复烦烦烦烦烦烦烦烦烦烦烦烦烦烦烦正文啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶反反复复烦烦烦烦烦烦烦烦烦烦烦烦烦烦烦正文啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶反反复复烦烦烦烦烦烦烦烦烦烦烦烦烦烦烦")
                        p("正文啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊顶顶顶顶顶顶顶顶顶顶顶顶顶顶顶反反复复烦烦烦烦烦烦烦烦烦烦烦烦烦烦烦")
                    }.build()
                    registerWidget(widget)
                    super.init()
                }
            }
        }*/

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
            mc.createWorldOpenFlows().loadLevel(this, levelName)
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
                WorldOptions(0, false, false)
            ) {
                it.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                    .value().createWorldDimensions();
            }
        }
    }


}