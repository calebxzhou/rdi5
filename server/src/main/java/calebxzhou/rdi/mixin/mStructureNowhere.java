package calebxzhou.rdi.mixin;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * calebxzhou @ 8/21/2025 11:09 PM
 */
//哪里都不生成结构

@Mixin(StructureCheck.class)
public class mStructureNowhere {
    @Overwrite
    private boolean canCreateStructure(ChunkPos pChunkPos, Structure pStructure) {
        return false;
    }
}

@Mixin(Structure.class)
class mStructureNowhere2 {
    @Overwrite
    private static boolean isValidBiome(Structure.GenerationStub pStub, Structure.GenerationContext pContext) {
        return false;
    }
}