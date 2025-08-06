package calebxzhou.rdi.mixin;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * calebxzhou @ 2025-03-23 23:22
 */
@Mixin(StructureCheck.class)
public class mStructureEverywhere {
    /*@Overwrite
    private boolean canCreateStructure(ChunkPos pChunkPos, Structure pStructure) {
        return true;
    }
*/}

@Mixin(Structure.class)
class mStructureEverywhere2 {
/*    @Overwrite
    private static boolean isValidBiome(Structure.GenerationStub pStub, Structure.GenerationContext pContext) {
        return true;
    }*/
}