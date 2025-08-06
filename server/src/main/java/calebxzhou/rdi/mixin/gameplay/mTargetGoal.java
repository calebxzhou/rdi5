package calebxzhou.rdi.mixin.gameplay;

import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * calebxzhou @ 2024-06-06 16:17
 */
@Mixin(TargetGoal.class)
public class mTargetGoal {
    @Overwrite
    public double getFollowDistance() {
        return 128;
    }
}
