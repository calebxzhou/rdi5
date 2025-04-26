package calebxzhou.rdi.util

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.item.Item
import net.neoforged.neoforge.registries.NeoForgeRegistries

/**
 * calebxzhou @ 2025-04-24 10:28
 */
val Item.chineseName
    get() = this.getName(defaultInstance) as MutableComponent
val Item.id
    get() = BuiltInRegistries.ITEM.getKey(this)