package calebxzhou.rdi.mixin;

import net.minecraft.data.worldgen.DimensionTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * calebxzhou @ 8/21/2025 11:03 PM
 */
@Mixin(DimensionTypes.class)
public class mOverworldHeight {
    //限制256高度
    @ModifyConstant(method = "bootstrap",constant = @Constant(intValue = -64))
    private static int RDI$OverworldMinY(int constant) {
        return 0;
    }
    @ModifyConstant(method = "bootstrap",constant = @Constant(intValue = 384))
    private static int RDI$OverworldH(int constant) {
        return 256;
    }
}
