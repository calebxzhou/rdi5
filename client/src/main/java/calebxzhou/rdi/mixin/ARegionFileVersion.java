package calebxzhou.rdi.mixin;

import net.minecraft.world.level.chunk.storage.RegionFileVersion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RegionFileVersion.class)
public interface ARegionFileVersion {
    @Invoker
    static RegionFileVersion invokeRegister(RegionFileVersion fileVersion) {
        return null;
    }
}
