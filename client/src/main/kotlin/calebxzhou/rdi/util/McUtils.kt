package calebxzhou.rdi.util

import calebxzhou.rdi.ui.WindowHandle
import com.mojang.blaze3d.platform.InputConstants
import io.netty.util.concurrent.DefaultThreadFactory
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.core.BlockPos
import net.minecraft.core.Registry
import net.minecraft.core.SectionPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.neoforged.neoforge.registries.NeoForgeRegistries
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
val BlockState.id
    get() = Block.BLOCK_STATE_REGISTRY.getId(this)
//y*256 + z*16 + x
val BlockPos.sectionIndex: Int
    get() = (y and 0xF shl 4 or (z and 0xF)) shl 4 or (x and 0xF)
val Minecraft.pressingEnter
    get() =  this pressingKey InputConstants.KEY_RETURN || this pressingKey InputConstants.KEY_NUMPADENTER
val ChunkPos.asInt
    get() = x.toShort().toInt() and 0xFFFF or ((z.toShort().toInt() and 0xFFFF) shl 16)