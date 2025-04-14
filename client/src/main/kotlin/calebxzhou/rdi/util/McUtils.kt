package calebxzhou.rdi.util

import net.minecraft.client.Minecraft

/**
 * calebxzhou @ 2025-04-14 14:59
 */

val mc: Minecraft
    get() = Minecraft.getInstance() ?: run {
        throw IllegalStateException("Minecraft Not Start !")
    }
fun renderThread(run: () -> Unit) {
    mc.execute(run)
}