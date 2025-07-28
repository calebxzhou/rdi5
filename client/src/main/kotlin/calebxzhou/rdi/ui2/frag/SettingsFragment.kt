package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.ui.screen.FovScreen
import calebxzhou.rdi.ui2.gridLayout
import calebxzhou.rdi.ui2.iconButton
import calebxzhou.rdi.ui2.mcScreen
import calebxzhou.rdi.ui2.paddingDp
import calebxzhou.rdi.util.*
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen
import net.minecraft.client.gui.screens.options.SoundOptionsScreen
import net.minecraft.client.gui.screens.options.controls.ControlsScreen
import net.minecraft.client.gui.screens.packs.PackSelectionScreen
import net.neoforged.neoforge.client.gui.ModListScreen
import org.embeddedt.embeddium.impl.gui.EmbeddiumVideoOptionsScreen
import org.lwjgl.opengl.GL

class SettingsFragment: RFragment("设置") {
    val options = mc.options
    val scr = this@SettingsFragment.mcScreen
    override fun initContent() {
        contentLayout.apply {
            gridLayout {
                paddingDp(16)
                // Add settings options here
                iconButton("plugin","模组") {
                    mc go ModListScreen(scr)
                }

                iconButton("resources","资源包") {
                    mc go PackSelectionScreen(mc.resourcePackRepository, {
                        options.updateResourcePacks(it)
                        mc.popGuiLayer()
                    }, mc.resourcePackDirectory, "选择资源包".mcComp)
                }
                iconButton("video","画质") {
                    renderThread {

                        GL.createCapabilities()
                        mc go EmbeddiumVideoOptionsScreen(scr, EmbeddiumVideoOptionsScreen.makePages())
                    }
                }
                iconButton("camera","视野") {
                    mc set FovScreen()
                }
                iconButton("sound",text = "音频"){
                    mc go SoundOptionsScreen(scr,options)
                }
                iconButton("controller",text = "键位"){
                    mc go ControlsScreen(scr,options)
                }
                iconButton("accessibility",text = "辅助"){
                    mc go AccessibilityOptionsScreen(scr,options)
                }
            }
        }
    }
}