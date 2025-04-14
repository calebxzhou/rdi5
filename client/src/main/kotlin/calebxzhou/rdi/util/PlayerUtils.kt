package calebxzhou.rdi.util

import net.minecraft.tags.TagKey
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import snownee.jade.overlay.RayTracing

/**
 * calebxzhou @ 2025-04-14 23:10
 */
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
val Player.lookingAtItemEntity: ItemEntity?
    get() {
        val entity = lookingAtEntity
        return if (entity != null && entity is ItemEntity) {
            entity
        } else null
    }
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

//轻量化itemstack 只有物品和数量
typealias LiteItemStack = Pair<Item,Int>
fun Player.bagHas(cond: (ItemStack) -> Boolean): Boolean {
    return inventory.hasAnyMatching(cond)
}
fun Player.bagHas(tag: TagKey<Item>, count: Int = 1): Boolean {
    return bagHas { it.`is`(tag) && it.count >= count }
}
infix fun Player.bagHas(item: Item): Boolean {
    return bagHas(item to 1)
}
infix fun Player.bagHasStack(item: ItemStack): Boolean {
    return bagHas { it == item }
}
//数量大于等于
infix fun Player.bagHas(lis: LiteItemStack): Boolean {
    return this.bagHas { it.`is`(lis.first) && it.count >= lis.second }
}

infix fun Player.handHas(item: Item): Boolean {
    return mainHandItem.`is`(item)
}
val Player.handsAir
    get() = mainHandItem.isEmpty
infix fun Player.handHas(itemStack: LiteItemStack): Boolean = mainHandItem.`is`(itemStack.first) && mainHandItem.count == itemStack.second
infix fun Player.handHas(itemTag: TagKey<Item>): Boolean = mainHandItem.`is`(itemTag)
infix fun Player.feetOn(block: Block): Boolean = level().getBlockState(blockPosition().below()).`is`(block)
infix fun Player.isLooking(block: Block): Boolean = lookingAtBlock?.`is`(block) == true
infix fun Player.isLooking(item: Item): Boolean = lookingAtItemEntity?.item?.`is`(item) == true

