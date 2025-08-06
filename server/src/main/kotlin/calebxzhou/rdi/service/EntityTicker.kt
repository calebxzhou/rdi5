package calebxzhou.rdi.service

import calebxzhou.rdi.lgr
import calebxzhou.rdi.mixin.AEntitySection
import calebxzhou.rdi.mixin.APersistentEntityManager
import calebxzhou.rdi.mixin.AServerLevel
import calebxzhou.rdi.util.mcs
import net.minecraft.core.SectionPos
import net.minecraft.core.component.DataComponentMap
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.level.Level
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent
import java.util.function.Consumer

object EntityTicker {
    @JvmStatic
    fun <T : Entity> tick(consumerEntity: Consumer<T>, entity: T) {
        try {
            //if (!TickInverter.isLagging)
            //没玩家不tick
            if (!entity.level().players().isEmpty() || entity.level() == mcs.getLevel(Level.END)) {
                consumerEntity.accept(entity)
            }
        } catch (e: Exception) {
            lgr.error("在${entity}tick entity错误！${e}原因：${e.message},${e.cause}。已经强制删除！")
            e.printStackTrace()
            if (e !is NullPointerException) {
                entity.discard()
            }
        }
    }

    fun onCreate(e: EntityJoinLevelEvent) {
            val entity = e.entity
            val cp = entity.chunkPosition()
            val level = (entity.level() as AServerLevel)
            val sectionStorage = (level.entityManager as APersistentEntityManager<*>).sectionStorage
            sectionStorage.getOrCreateSection(SectionPos.asLong(entity.blockPosition())).let { sect ->
                if (sect.size() > 768) {
                    //清所有的怪
                    (sect as AEntitySection<*>).storage.find(Monster::class.java).forEach { it.discard() }
                    //清没有nbt的物品
                    (sect as AEntitySection<*>).storage.find(ItemEntity::class.java)
                        .filter { it.item.components== DataComponentMap.EMPTY }
                        //.maxBy { it.age }
                        .forEach {
                            it.discard()
                        }
                }
            }

    }


}