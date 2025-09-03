package calebxzhou.rdi.util

import calebxzhou.rdi.lgr
import calebxzhou.rdi.ui2.frag.RFragment
import icyllis.modernui.fragment.Fragment
import icyllis.modernui.mc.MuiModApi
import icyllis.modernui.mc.ScreenCallback
import icyllis.modernui.mc.neoforge.MuiForgeApi
import io.netty.util.concurrent.DefaultThreadFactory
import net.minecraft.Util
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.client.server.IntegratedServer
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import org.lwjgl.glfw.GLFW
import java.util.*
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
val mcs: IntegratedServer?
    get() = mc.singleplayerServer
fun renderThread(run: () -> Unit) {
    mc.execute(run)
}
fun uiThread(run: () -> Unit) {
    MuiModApi.postToUiThread(run)
}
fun rdiAsset(path: String) = ResourceLocation.fromNamespaceAndPath("rdi",path)
fun mcAsset(path: String) = ResourceLocation.withDefaultNamespace(path)
val ResourceLocation.isTextureReady
    get() = mc.textureManager.getTexture(this, MissingTextureAtlasSprite.getTexture()) != MissingTextureAtlasSprite.getTexture()
fun copyToClipboard(s: String) {
    GLFW.glfwSetClipboardString(WindowHandle, s)
}
val screenLayer = Stack<Screen>()
//go加入forge gui layer, set不
infix fun Minecraft.go(screen: Screen?) {
  /*  if(screen==null)
        RPauseScreen.unpause()*/
    renderThread {
        if (screen != null) {
            mc.pushGuiLayer(screen)
        }else setScreen(screen)
    }
} //前screen=null时，默认使用当前screen
fun Minecraft. go(fragment: Fragment, prevScreen: Screen?=null){
    renderThread {
        val screen: Screen = MuiForgeApi.get().createScreen(fragment,null,prevScreen?:mc.screen)
        mc set screen
    }
}
infix fun Minecraft. go(fragment: RFragment){
    renderThread {
        val screen: Screen = MuiForgeApi.get().createScreen(fragment, object:ScreenCallback {
            override fun shouldClose(): Boolean {
                return fragment.closable
            }
        },mc.screen)
        mc set screen
    }
}
infix fun Minecraft.set(screen: Screen?) {
    renderThread {
        setScreen(screen)
    }
}
val BlockState.id
    get() = Block.BLOCK_STATE_REGISTRY.getId(this)
//y*256 + z*16 + x
val BlockPos.sectionIndex: Int
    get() = (y and 0xF shl 4 or (z and 0xF)) shl 4 or (x and 0xF)
 val ChunkPos.asInt
    get() = x.toShort().toInt() and 0xFFFF or ((z.toShort().toInt() and 0xFFFF) shl 16)

val Int.asChunkPos: ChunkPos
    get() {
        val x = this and 0xFFFF
        val z = (this shr 16) and 0xFFFF
        return ChunkPos(x.toShort().toInt(), z.toShort().toInt())
    }


fun String.openAsUri(){
    Util.getPlatform().openUri(this)
}
fun Minecraft.addChatMessage(msg: String) {
    msg.split("\n").forEach {
        gui.chat.addMessage(it.mcComp)
    }
}

fun Minecraft.addChatMessage(msg: Component) {
    gui.chat.addMessage(msg)
}
val MinecraftServer.playingLevel: ServerLevel?
    get() = mc.level?.let { getLevel(it.dimension()) }
fun Minecraft.sendCommand(cmd: String){
    mc.player?.connection?.sendCommand(cmd)?:let {
        lgr.warn("no connection fail send command")
    }
}