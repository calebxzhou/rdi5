package calebxzhou.rdi.service

import calebxzhou.rdi.lgr
import net.minecraft.world.entity.Entity
import java.util.function.Consumer

object EntityTicker {
    @JvmStatic
    fun <T : Entity> tick(consumerEntity: Consumer<T>, entity: T) {
        try {
            //if (!TickInverter.isLagging)
            //没玩家不tick
            /*if (!entity.level().players().isEmpty() || entity.level() == mcs.getLevel(Level.END)) {
                consumerEntity.accept(entity)
            }*/
            consumerEntity.accept(entity)
        } catch (e: Exception) {
            lgr.error("在${entity}tick entity错误！${e}原因：${e.message},${e.cause}。已经强制删除！")
            e.printStackTrace()
            if (e !is NullPointerException) {
                entity.discard()
            }
        }
    }




}