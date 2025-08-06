package calebxzhou.rdi.service

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import net.neoforged.neoforge.client.settings.KeyConflictContext
import net.neoforged.neoforge.client.settings.KeyModifier
import org.lwjgl.glfw.GLFW

object RKeyBinds {
    val HOME =
        KeyMapping(
            "回家",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.getKey(GLFW.GLFW_KEY_H,-1),
            "rdi"
        )

    val MCMOD =
        KeyMapping(
            "mc百科",
            KeyConflictContext.UNIVERSAL,
            KeyModifier.ALT,
            InputConstants.getKey(GLFW.GLFW_KEY_B,-1),
            "rdi"
        )
}