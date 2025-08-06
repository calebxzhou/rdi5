package calebxzhou.rdi.util

import calebxzhou.rdi.service.EnglishStorage
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.item.Item

/**
 * calebxzhou @ 2025-04-24 10:28
 */
val Item.chineseName
    get() = this.getName(defaultInstance) as MutableComponent
val Item.englishName
    get() = EnglishStorage[descriptionId].mcComp
val Item.id
    get() = BuiltInRegistries.ITEM.getKey(this)