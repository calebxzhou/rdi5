package calebxzhou.rdi.util

import calebxzhou.rdi.ui.WindowHandle
import com.mojang.blaze3d.platform.InputConstants
import io.netty.util.concurrent.DefaultThreadFactory
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.ChunkPos
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * calebxzhou @ 2025-04-14 14:59
 */
val isMcStarted
    get() = Minecraft.getInstance() != null
val threadPool: ExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), DefaultThreadFactory("RDI-ThreadPool"))
//线程池 异步
fun mcAsync(todo: () -> Unit) {
    threadPool.execute(todo)
}
val mc: Minecraft
    get() = Minecraft.getInstance() ?: run {
        throw IllegalStateException("Minecraft Not Start !")
    }
fun renderThread(run: () -> Unit) {
    mc.execute(run)
}
fun rdiAsset(path: String) = ResourceLocation.fromNamespaceAndPath("rdi",path)
fun mcAsset(path: String) = ResourceLocation.withDefaultNamespace(path)
val ResourceLocation.isTextureReady
    get() = mc.textureManager.getTexture(this, MissingTextureAtlasSprite.getTexture()) != MissingTextureAtlasSprite.getTexture()


//go加入forge gui layer, set不
infix fun Minecraft.go(screen: Screen?) {
  /*  if(screen==null)
        RPauseScreen.unpause()*/
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
infix fun Minecraft.pressingKey(keyCode: Int): Boolean {
    return InputConstants.isKeyDown(WindowHandle, keyCode)
}
val Minecraft.pressingEnter
    get() =  this pressingKey InputConstants.KEY_RETURN || this pressingKey InputConstants.KEY_NUMPADENTER
val ChunkPos.asInt
    get() = x.toShort().toInt() and 0xFFFF or ((z.toShort().toInt() and 0xFFFF) shl 16)