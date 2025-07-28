package calebxzhou.rdi.ui.screen

import calebxzhou.rdi.option.RSettings
import calebxzhou.rdi.ui.component.RCheckbox
import calebxzhou.rdi.ui.general.HAlign
import calebxzhou.rdi.ui.layout.gridLayout
import calebxzhou.rdi.ui.layout.linearLayout
import calebxzhou.rdi.util.*
import com.mojang.blaze3d.platform.InputConstants
import icyllis.modernui.mc.neoforge.CenterFragment2
import net.minecraft.client.Options
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen
import net.minecraft.client.gui.screens.options.ChatOptionsScreen
import net.minecraft.client.gui.screens.options.OptionsScreen
import net.minecraft.client.gui.screens.options.SoundOptionsScreen
import net.minecraft.client.gui.screens.options.controls.ControlsScreen
import net.minecraft.client.gui.screens.packs.PackSelectionScreen
import net.neoforged.neoforge.client.gui.ModListScreen
import org.embeddedt.embeddium.impl.gui.EmbeddiumVideoOptionsScreen

class   RSettingsScreen(val options: Options): RScreen("设置") {
    override fun init() {
        linearLayout(this) {
            checkBox("bootResize","启动时自动调整窗口大小",   RSettings.now.autoAdjustWindowSize)
        }
        gridLayout(this, hAlign = HAlign.CENTER,y=120, maxColumns = 4) {

            button("plugin",text = "模组"){
                mc go ModListScreen(this@RSettingsScreen)
            }
            button("resources",text = "资源包"){
                mc go PackSelectionScreen(mc.resourcePackRepository,{
                    options.updateResourcePacks(it)
                    mc.popGuiLayer()
                },mc.resourcePackDirectory, "选择资源包".mcComp)
            }
            button("video",text = "画质"){
                mc go EmbeddiumVideoOptionsScreen(this@RSettingsScreen,EmbeddiumVideoOptionsScreen.makePages())
            }
            button("camera",text = "视野"){
                mc set FovScreen()
            }

        }
        gridLayout(this, hAlign = HAlign.CENTER,y=160){
            button("sound",text = "音频"){
                mc go SoundOptionsScreen(this@RSettingsScreen,options)
            }
            button("controller",text = "键位"){
                mc go ControlsScreen(this@RSettingsScreen,options)
            }
            button("accessibility",text = "辅助"){
                mc go AccessibilityOptionsScreen(this@RSettingsScreen,options)
            }
        }


        super.init()
    }

    override fun doRender(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {

    }
    override fun tick() {
        if(mc pressingKey InputConstants.KEY_L){
            mc go ChatOptionsScreen(this@RSettingsScreen,options)
        }
        if(mc pressingKey InputConstants.KEY_O){
            mc go  OptionsScreen(this@RSettingsScreen,options)
        }
        if(mc pressingKey InputConstants.KEY_K){
            mc.go(CenterFragment2(),this@RSettingsScreen)
        }

        RSettings.now.autoAdjustWindowSize=ofWidget<RCheckbox>("bootResize").selected()
        super.tick()
    }
    override fun removed() {
        save()
    }

    override fun onClose() {
        super.onClose()
        save()
    }
    fun save(){
        RSettings.save()
        options.save()
    }
}