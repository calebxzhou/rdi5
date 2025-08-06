package calebxzhou.rdi.mixin.gameplay;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * calebxzhou @ 2024-06-06 16:01
 */
@Mixin(LivingEntity.class)
public class mEntitySight {
    @ModifyConstant(method = "hasLineOfSight",constant = @Constant(doubleValue = 128.0))
    private double entitySight(double d){
        return 256;
    }
    //不进行追踪 节约性能
    /*@Inject(method = "hasLineOfSight",at=@At(value = "RETURN",ordinal = 2), cancellable = true)
    private void sight(Entity entity, CallbackInfoReturnable<Boolean> cir){
        cir.setReturnValue(true);
    }*/
}
