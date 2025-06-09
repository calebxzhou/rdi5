package calebxzhou.rdi.service

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.KeyMapping
import net.neoforged.neoforge.client.settings.KeyConflictContext
import net.neoforged.neoforge.client.settings.KeyModifier
import org.lwjgl.glfw.GLFW

object RKeyBinds {
    val HOME =
        KeyMapping(
            "key.rdi.home",
            KeyConflictContext.IN_GAME,
            KeyModifier.NONE,
            InputConstants.getKey(GLFW.GLFW_KEY_H,-1),
            "key.rdi.cate"
        )

    val MCMOD =
        KeyMapping(
            "key.rdi.mcmod",
            KeyConflictContext.GUI,
            KeyModifier.ALT,
            InputConstants.getKey(GLFW.GLFW_KEY_B,-1),
            "key.rdi.cate"
        )

}