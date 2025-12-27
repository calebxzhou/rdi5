package calebxzhou.rdi.client.mc

import calebxzhou.mykotutils.log.Loggers
import calebxzhou.mykotutils.log.Loggers.provideDelegate
import com.google.common.net.HostAndPort
import com.mojang.util.UndashedUuid
import net.minecraft.Util
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.multiplayer.resolver.ServerAddress
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite
import net.minecraft.client.server.IntegratedServer
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import java.util.*

/**
 * calebxzhou @ 2025-04-14 14:59
 */
val String.mcComp: MutableComponent
    get() = Component.literal(this)
val isMcStarted
    get() = isClassLoaded("net.minecraft.client.Minecraft") && Minecraft.getInstance() != null
private val lgr by Loggers
val mc: Minecraft
    get() = Minecraft.getInstance() ?: run {
        throw IllegalStateException("Minecraft Not Start !")
    }
//游戏未启动则null
val mcn : Minecraft
    get() = Minecraft.getInstance()
val mcs: IntegratedServer?
    get() = mc.singleplayerServer
fun mainThread(run: () -> Unit) {
    mc.execute(run)
}

fun rdiAsset(path: String) = ResourceLocation.fromNamespaceAndPath("rdi",path)
fun mcAsset(path: String) = ResourceLocation.withDefaultNamespace(path)
val ResourceLocation.isTextureReady
    get() = mc.textureManager.getTexture(this, MissingTextureAtlasSprite.getTexture()) != MissingTextureAtlasSprite.getTexture()

infix fun Minecraft.set(screen: Screen?) {
    mainThread {
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
fun Minecraft.addChatMessage(msg: String)= mainThread {
    msg.split("\n").forEach {
        gui.chat.addMessage(it.mcComp)
    }
}
val Item.id
    get() = BuiltInRegistries.ITEM.getKey(this)
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
fun Minecraft.connectServer(ip:String){
    ConnectScreen.startConnecting(
        TitleScreen(),
        this,
        HostAndPort.fromString(ip).let { ServerAddress(it.host, it.port) },
        ServerData("rdi",ip, ServerData.Type.OTHER),
        false,
        null
    )
}
private fun parseUuidFlexible(raw: String): UUID? {
    return try {
        UUID.fromString(raw)
    } catch (_: IllegalArgumentException) {
        try {
            UndashedUuid.fromStringLenient(raw)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
val Player.lookingAtBlock: BlockState?
    get() {
        val hit = pick(20.0, 0.0f, true)
        if (hit.type == HitResult.Type.BLOCK) {
            val bpos = (hit as BlockHitResult).blockPos
            val bstate = level().getBlockState(bpos)
            return bstate
        }
        return null
    }
val Player.lookingAtBlockEntity: BlockEntity?
    get() {
        val hit = pick(20.0, 0.0f, true)
        if (hit.type == HitResult.Type.BLOCK) {
            val bpos = (hit as BlockHitResult).blockPos
            val bstate = level().getBlockEntity(bpos)
            return bstate
        }
        return null
    }
/*val Player.lookingAtItemEntity: ItemEntity?
    get() {
        val entity = lookingAtEntity
        return if (entity != null && entity is ItemEntity) {
            entity
        } else null
    }*/
/*
val Player.lookingAtEntity: Entity?
    get() {
        val hit = RayTracing.INSTANCE.rayTrace(this, mc.player?.entityInteractionRange()?.toDouble() ?: 0.0,
            mc.timer.gameTimeDeltaTicks.toDouble()
        )
        if (hit?.type == HitResult.Type.ENTITY) {
            return (hit as EntityHitResult).entity
        }
        return null
    }
*/

fun Player.bagHas(cond: (ItemStack) -> Boolean): Boolean {
    return inventory.hasAnyMatching(cond)
}

infix fun Player.handHas(item: Item): Boolean {
    return mainHandItem.`is`(item)
}
val Player.handsAir
    get() = mainHandItem.isEmpty
infix fun Player.handHas(itemTag: TagKey<Item>): Boolean = mainHandItem.`is`(itemTag)
infix fun Player.feetOn(block: Block): Boolean = level().getBlockState(blockPosition().below()).`is`(block)
infix fun Player.isLooking(block: Block): Boolean = lookingAtBlock?.`is`(block) == true
//infix fun Player.isLooking(item: Item): Boolean = lookingAtItemEntity?.item?.`is`(item) == true

