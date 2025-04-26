package calebxzhou.rdi.ui.screen

import calebxzhou.rdi.option.RSettings
import calebxzhou.rdi.ui.component.RCheckbox
import calebxzhou.rdi.ui.component.RScreen
import calebxzhou.rdi.ui.general.HAlign
import calebxzhou.rdi.ui.layout.gridLayout
import calebxzhou.rdi.ui.layout.linearLayout
import calebxzhou.rdi.util.*
import calebxzhou.rdi.util.mc.mcComp
import com.mojang.blaze3d.platform.InputConstants
import me.jellysquid.mods.sodium.client.gui.SodiumOptionsGUI
import net.minecraft.client.Options
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.*
import net.minecraft.client.gui.screens.controls.ControlsScreen
import net.minecraft.client.gui.screens.packs.PackSelectionScreen
import net.minecraftforge.client.gui.ModListScreen

class RSettingsScreen(val options: Options): RScreen("设置") {
    override fun init() {
        /*Account.now?.let { account ->
            RPlayerHeadButton(account, UiWidth/2- (Font.width(account.name.mcComp)+14+5)/2,40,){}.also { registerWidget(it) }
            gridLayout(this,hAlign = HAlign.CENTER,y=80) {
                iconButton("basic_info",text = "资料"){
                    mc goScreen ControlsScreen(this@RSettingsScreen,options)
                }
                iconButton("clothes",text = "皮肤"){
                    mc goScreen ControlsScreen(this@RSettingsScreen,options)
                }
            }
        }*/
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
                mc go SodiumOptionsGUI(this@RSettingsScreen)
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