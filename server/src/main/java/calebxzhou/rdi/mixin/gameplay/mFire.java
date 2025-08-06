package calebxzhou.rdi.mixin.gameplay;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * calebxzhou @ 2024-05-26 9:49
 */
@Mixin(FireBlock.class)
public class mFire {

    //快速着火
    @Overwrite
    private static int getFireTickDelay(RandomSource randomSource){
        return 15;
    }
}
