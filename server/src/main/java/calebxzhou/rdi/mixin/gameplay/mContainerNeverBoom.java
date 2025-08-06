package calebxzhou.rdi.mixin.gameplay;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EntityBasedExplosionDamageCalculator;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * calebxzhou @ 2024-05-26 11:15
 */
@Mixin(EntityBasedExplosionDamageCalculator.class)
public class mContainerNeverBoom {
    //方块实体不会被炸
    @Overwrite
    public boolean shouldBlockExplode(Explosion explosion, BlockGetter world, BlockPos pos, BlockState state, float power) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        return blockEntity == null;
    }
}
