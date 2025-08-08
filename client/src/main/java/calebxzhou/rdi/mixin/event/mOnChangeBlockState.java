package calebxzhou.rdi.mixin.event;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * calebxzhou @ 2025-04-16 13:32
 */
@Mixin(LevelChunk.class)
public class mOnChangeBlockState {

    @Shadow @Final
    Level level;

 /*   @Inject(method = "setBlockState",at=@At(value = "RETURN",ordinal = 3))
    private void RDI$setBLockSTate(BlockPos pos, BlockState state, boolean isMoving, CallbackInfoReturnable<BlockState> cir){
        var chunk = (LevelChunk)(Object)this;
        var event = new BlockStateChangedEvent(level,chunk,pos,state,isMoving);
        RGameEvents.onBlockStateChange(event);
    }*/
}
