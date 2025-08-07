package calebxzhou.rdi.util

import io.netty.util.concurrent.DefaultThreadFactory
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.client.server.IntegratedServer
import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
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
lateinit var mcs: MinecraftServer
fun mainThread(run: () -> Unit) {
    mc.execute(run)
}
val BlockState.id
    get() = Block.BLOCK_STATE_REGISTRY.getId(this)
//y*256 + z*16 + x
val BlockPos.sectionIndex: Int
    get() = (y and 0xF shl 4 or (z and 0xF)) shl 4 or (x and 0xF)
 val ChunkPos.asInt
    get() = x.toShort().toInt() and 0xFFFF or ((z.toShort().toInt() and 0xFFFF) shl 16)

val ServerPlayer.dimensionName
    get() = level().dimensionName
val ServerPlayer.nickname
    get() = displayName?.string?:""
val Int.asChunkPos: ChunkPos
    get() {
        val x = this and 0xFFFF
        val z = (this shr 16) and 0xFFFF
        return ChunkPos(x.toShort().toInt(), z.toShort().toInt())
    }


val MinecraftServer.playingLevel: ServerLevel?
    get() = mc.level?.let { getLevel(it.dimension()) }

fun CommandSourceStack.chat(msg: String) {
    msg.split("\n").forEach { sendSystemMessage((it).mcComp) }
}

fun ServerPlayer.chat(msg: String) {
    msg.split("\n").forEach { sendSystemMessage((it).mcComp) }
}