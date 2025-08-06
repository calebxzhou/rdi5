package calebxzhou.rdi.mixin.gameplay;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.CauldronBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * calebxzhou @ 2024-05-26 11:14
 */

@Mixin(CauldronBlock.class)
public class mCauldron {
    @Overwrite
    public static boolean shouldHandlePrecipitation(Level level, Biome.Precipitation precipitation) {
        return precipitation == Biome.Precipitation.RAIN || precipitation == Biome.Precipitation.SNOW;
    }
}
