package calebxzhou.rdi.mixin.gameplay;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/**
 * calebxzhou @ 2024-05-26 11:18
 */
@Mixin(IronGolem.class)
public abstract class mIronGolem {

    @Shadow public abstract boolean isPlayerCreated();

    //铁傀儡增加血量
    @Overwrite
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 200.0)
                .add(Attributes.MOVEMENT_SPEED, 0.6)
                .add(Attributes.KNOCKBACK_RESISTANCE, 2.0)
                .add(Attributes.ATTACK_DAMAGE, 25.0);
    }
    //什么都攻击
    /*@Overwrite
    public boolean canAttackType(EntityType<?> entityType) {
        if (isPlayerCreated() && entityType == EntityType.PLAYER) {
            return false;
        }else{
            return true;
        }
    }*/


}