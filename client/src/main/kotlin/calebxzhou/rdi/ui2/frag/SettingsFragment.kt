package calebxzhou.rdi.ui2.frag

import calebxzhou.rdi.ui2.*
import calebxzhou.rdi.util.go
import calebxzhou.rdi.util.mc
import calebxzhou.rdi.util.mcComp
import calebxzhou.rdi.util.renderThread
import icyllis.modernui.view.Gravity
import icyllis.modernui.widget.LinearLayout
import net.caffeinemc.mods.sodium.client.gui.SodiumOptionsGUI
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen
import net.minecraft.client.gui.screens.options.SoundOptionsScreen
import net.minecraft.client.gui.screens.options.controls.ControlsScreen
import net.minecraft.client.gui.screens.packs.PackSelectionScreen
import net.neoforged.neoforge.client.gui.ModListScreen

class SettingsFragment : RFragment("设置") {
    val options
        get() = mc.options
    override var fragSize: FragmentSize
        get() = FragmentSize.SMALL
        set(value) {}
    init {
        contentViewInit = {
            linearLayout {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = linearLayoutParam(PARENT, PARENT)
                linearLayout {
                    gravity = Gravity.CENTER
                    paddingDp(16)
                    // First row - 3 buttons
                    button("\uEB2D 模组") { mc go ModListScreen(mc.screen) }
                    button("\uEB29  资源包") {
                        mc go PackSelectionScreen(mc.resourcePackRepository, {
                            options.updateResourcePacks(it)
                            mc.popGuiLayer()
                        }, mc.resourcePackDirectory, "选择资源包".mcComp)
                    }
                    button("\uEA7A  画质") {
                        renderThread {
                            mc go SodiumOptionsGUI.createScreen(mc.screen)
                        }
                    }
                }
                linearLayout {
                    gravity = Gravity.CENTER
                    paddingDp(16)
                    button("\uF03D  视野") {
                        goto(FovFragment())
                    }
                    button("\uE638  音频") {
                        mc go SoundOptionsScreen(mc.screen, options)
                    }
                    button("\uF11C  键位") {
                        mc go ControlsScreen(mc.screen, options)
                    }
                    button("\uF29A  辅助") {
                        mc go AccessibilityOptionsScreen(mc.screen, options)
                    }
                }
            }
        }
    }

}