package calebxzhou.rdi.mixin.gameplay;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.GlowSquid;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * calebxzhou @ 2024-05-26 11:16
 */

@Mixin(GlowSquid.class)
public class mGlowSquid {

    /**
     * 发光鱿鱼减少生成
     * @author
     */
    @Overwrite
    public static boolean checkGlowSquidSpawnRules(
            EntityType<? extends LivingEntity> glowSquid, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random
    ){    return level.getBlockState(pos).is(Blocks.WATER) && pos.getY() <= 127
                &&
                random.nextInt(0,12)==0;
    }

}
