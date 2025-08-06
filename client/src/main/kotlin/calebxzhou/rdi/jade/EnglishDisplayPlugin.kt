package calebxzhou.rdi.jade

import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.Block
import snownee.jade.api.IWailaClientRegistration
import snownee.jade.api.IWailaPlugin
import snownee.jade.api.WailaPlugin

@WailaPlugin
class EnglishDisplayPlugin : IWailaPlugin {
    override fun registerClient(registration: IWailaClientRegistration) {
        registration.registerBlockComponent(EnglishBlockDisplayProvider.INSTANCE,Block::class.java)
        registration.registerEntityComponent(EnglishEntityDisplayProvider.INSTANCE,Entity::class.java)

    }
}