package calebxzhou.rdi.mixin.gameplay;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * calebxzhou @ 2024-06-06 17:00
 */
@Mixin(Level.class)
public abstract class mExplosion {
    @Shadow
    public abstract Explosion explode( Entity source,  DamageSource damageSource,  ExplosionDamageCalculator damageCalculator, double x, double y, double z, float radius, boolean fire, Level.ExplosionInteraction explosionInteraction);

    //所有爆炸带火
    @Overwrite
    public Explosion explode( Entity source, double x, double y, double z, float radius, Level.ExplosionInteraction explosionInteraction) {
        return explode(source, null, null, x, y, z, radius, true, explosionInteraction);
    }
}

@Mixin(Explosion.class)
class mHigherExplosion {
    /*@Redirect(method = "explode", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private void higherExplode(Entity instance, Vec3 deltaMovement) {
        if (instance instanceof Player)
           instance.setDeltaMovement(deltaMovement.add(20, 200, 20));
    }*/
}