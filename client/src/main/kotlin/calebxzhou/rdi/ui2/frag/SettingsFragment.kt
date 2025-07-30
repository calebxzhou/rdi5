package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.ui.screen.FovScreen
import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.util.*
import icyllis.modernui.view.Gravity
import icyllis.modernui.widget.LinearLayout
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen
import net.minecraft.client.gui.screens.options.SoundOptionsScreen
import net.minecraft.client.gui.screens.options.controls.ControlsScreen
import net.minecraft.client.gui.screens.packs.PackSelectionScreen
import net.neoforged.neoforge.client.gui.ModListScreen
import org.embeddedt.embeddium.impl.gui.EmbeddiumVideoOptionsScreen
import org.lwjgl.opengl.GL

class SettingsFragment: RFragment("设置") {
    val options = mc.options
     override fun initContent() {
        contentLayout.apply {
            linearLayout {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = linearLayoutParam(PARENT, PARENT)

                linearLayout {
                    gravity = Gravity.CENTER
                    paddingDp(16)


                    // First row - 3 buttons
                    iconButton("plugin", "模组") {
                        mc go ModListScreen(mc.screen)
                    }
                    iconButton("resources", "资源包") {
                        mc go PackSelectionScreen(mc.resourcePackRepository, {
                            options.updateResourcePacks(it)
                            mc.popGuiLayer()
                        }, mc.resourcePackDirectory, "选择资源包".mcComp)
                    }
                    iconButton("video", "画质") {
                        renderThread {
                            GL.createCapabilities()
                            mc go EmbeddiumVideoOptionsScreen(mc.screen, EmbeddiumVideoOptionsScreen.makePages())
                        }
                    }
                }
                linearLayout {
                    gravity = Gravity.CENTER
                    // Second row - 4 buttons
                    iconButton("camera","视野") {
                        mc set FovScreen()
                    }
                    iconButton("sound",text = "音频"){
                        mc go SoundOptionsScreen(mc.screen,options)
                    }
                    iconButton("controller",text = "键位"){
                        mc go ControlsScreen(mc.screen,options)
                    }
                    iconButton("accessibility",text = "辅助"){
                        mc go AccessibilityOptionsScreen(mc.screen,options)
                    }
                }
            }
        }
    }
}