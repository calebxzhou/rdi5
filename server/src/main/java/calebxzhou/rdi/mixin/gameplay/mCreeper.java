package calebxzhou.rdi.mixin.gameplay;

import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/**
 * calebxzhou @ 2024-05-26 9:45
 */
@Mixin(Creeper.class)
public class mCreeper {
    @Shadow
    @Mutable
    private int explosionRadius = 1;
    @Shadow
    @Mutable
    private int maxSwell = 20;

    @Overwrite
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MOVEMENT_SPEED, 0.5);
    }
}
