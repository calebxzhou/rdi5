package calebxzhou.rdi.util

import net.minecraft.network.chat.Component

/**
 * calebxzhou @ 2025-08-05 15:28
 */
val Font
    get() = mc.font
val McWindowHandle
    get() = mc.window.window
val UiWidth
    get() = mc.window.guiScaledWidth
val UiHeight
    get() = mc.window.guiScaledHeight
val mcWidth
    get() = mc.window.width
val mcHeight
    get() = mc.window.height
val ScreenWidth
    get() = mc.window.screenWidth
val ScreenHeight
    get() = mc.window.screenHeight
val mcUIScale
    get() = mc.window.guiScale
val LineHeight
    get() = Font.lineHeight
val Component.width
    get() = Font.width(this)
val String.width
    get() = Font.width(this)
val CenterY
    get() = (UiHeight- Font.lineHeight)/2
val CenterX
    get() = {text:String? -> (UiWidth-(text?.width?:0))/2}
val MouseX
    get() = (mc.mouseHandler.xpos() * UiWidth / ScreenWidth).toInt()
val MouseY
    get() = (mc.mouseHandler.ypos() * UiHeight / ScreenHeight).toInt()
