package calebxzhou.rdi.mixin.gameplay;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * calebxzhou @ 2024-06-21 16:47
 */
@Mixin(ItemEntity.class)
public class mItemEntity {
    //一分钟掉落物消失
    @Shadow @Final @Mutable
    private static final int LIFETIME = 20*60;
    @Shadow public int lifespan;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V",at=@At("TAIL"))
    private void RDI$QuickREmoveItem(EntityType entityType, Level level, CallbackInfo ci){
        lifespan=LIFETIME;
    }
    @Inject(method = "<init>(Lnet/minecraft/world/entity/item/ItemEntity;)V",at=@At("TAIL"))
    private void RDI$QuickREmoveItem2(ItemEntity other, CallbackInfo ci){
        lifespan=LIFETIME;
    }
    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V",at=@At("TAIL"))
    private void RDI$QuickREmoveItem3(Level level, double posX, double posY, double posZ, ItemStack itemStack, CallbackInfo ci){
        lifespan=LIFETIME;
    }
    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;DDD)V",at=@At("TAIL"))
    private void RDI$QuickREmoveItem4(Level level, double posX, double posY, double posZ, ItemStack itemStack, double deltaX, double deltaY, double deltaZ, CallbackInfo ci){
        lifespan=LIFETIME;
    }
}
