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
    val options = mc.options

    init {
        bottomOptionsConfig = {
            "\uEB2D 模组" with { mc go ModListScreen(mc.screen) }
            "\uEB29  资源包" with {
                mc go PackSelectionScreen(mc.resourcePackRepository, {
                    options.updateResourcePacks(it)
                    mc.popGuiLayer()
                }, mc.resourcePackDirectory, "选择资源包".mcComp)
            }
            "\uEA7A  画质" with {
                renderThread {
                    mc go SodiumOptionsGUI.createScreen(mc.screen)
                }
            }

            "\uF03D  视野" with  {
                mc go FovFragment()
            }
            "\uE638  音频" with {
                mc go SoundOptionsScreen(mc.screen, options)
            }
            "\uF11C  键位" with {
                mc go ControlsScreen(mc.screen, options)
            }
            "\uF29A  辅助" with {
                mc go AccessibilityOptionsScreen(mc.screen, options)
            }
        }
    }

    override fun initContent() {
        contentLayout.apply {
            linearLayout {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = linearLayoutParam(PARENT, PARENT)

                linearLayout {
                    gravity = Gravity.CENTER
                    paddingDp(16)


                }
                linearLayout {

                }
            }
        }
    }
}