package calebxzhou.rdi.util

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen

/**
 * calebxzhou @ 2025-04-14 14:59
 */
val Font
    get() = mc.font
val WindowHandle
    get() = mc.window.window
val UiWidth
    get() = mc.window.guiScaledWidth
val UiHeight
    get() = mc.window.guiScaledHeight
infix fun Minecraft.go(screen: Screen?) {
    execute {
        if (screen != null) {
            mc.pushGuiLayer(screen)
        }else setScreen(screen)
    }
}
infix fun Minecraft.set(screen: Screen?) {
    execute {
        setScreen(screen)
    }
}