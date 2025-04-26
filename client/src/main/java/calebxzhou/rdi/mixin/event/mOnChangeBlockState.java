package calebxzhou.rdi.mixin.event;

import calebxzhou.rdi.event.BlockStateChangedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * calebxzhou @ 2025-04-16 13:32
 */
@Mixin(LevelChunk.class)
public class mOnChangeBlockState {

    @Shadow @Final
    Level level;

    @Inject(method = "setBlockState",at=@At(value = "RETURN",ordinal = 3))
    private void RDI$setBLockSTate(BlockPos pos, BlockState state, boolean isMoving, CallbackInfoReturnable<BlockState> cir){
        var chunk = (LevelChunk)(Object)this;
        var event = new BlockStateChangedEvent(level,chunk,pos,state,isMoving);
        NeoForge.EVENT_BUS.post(event);
    }
}
