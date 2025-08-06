package calebxzhou.rdi.mixin.jade;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import snownee.jade.addon.core.CorePlugin;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.IWailaClientRegistration;

/**
 * calebxzhou @ 2024-10-16 21:49
 */
//jade不显示mod名称
@Mixin(CorePlugin.class)
public class mNoDisplayModName {
    @Redirect(remap = false, method = "registerClient",at= @At(ordinal = 1, value = "INVOKE", target = "Lsnownee/jade/api/IWailaClientRegistration;registerBlockComponent(Lsnownee/jade/api/IComponentProvider;Ljava/lang/Class;)V"))
    private void RDI$NoDisplayModName1(IWailaClientRegistration instance, IComponentProvider<BlockAccessor> blockAccessorIComponentProvider, Class<? extends Block> aClass){

    }
    @Redirect(remap = false, method = "registerClient",at= @At( ordinal = 1,value = "INVOKE", target = "Lsnownee/jade/api/IWailaClientRegistration;registerEntityComponent(Lsnownee/jade/api/IComponentProvider;Ljava/lang/Class;)V" ))
    private void RDI$NoDisplayModName2(IWailaClientRegistration instance, IComponentProvider<EntityAccessor> entityAccessorIComponentProvider, Class<? extends Entity> aClass){

    }
}
