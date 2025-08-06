package calebxzhou.rdi.mixin.gameplay;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.WanderingTraderSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * calebxzhou @ 2024-05-26 10:58
 */
//更多的流浪商人
@Mixin(WanderingTraderSpawner.class)
public abstract class mTraders {
    @ModifyConstant(method = "<init>(Lnet/minecraft/world/level/storage/ServerLevelData;)V",
            constant = @Constant(intValue = 24000))
    private int modifyConstSpawnDelay(int constant){
        return 3200;
    }
/*
    @ModifyConstant(method = "tick(Lnet/minecraft/server/level/ServerLevel;ZZ)I",
            constant = @Constant(intValue = 75))
    private int modifyConstMaxPercent(int constant){
        return 100;
    }*/
    @Redirect(method = "spawn(Lnet/minecraft/server/level/ServerLevel;)Z",
            at = @At(value = "INVOKE",target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int modNext(RandomSource instance, int i){
        return 0;
    }

}
