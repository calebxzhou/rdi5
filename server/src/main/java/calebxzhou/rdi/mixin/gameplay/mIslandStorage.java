package calebxzhou.rdi.mixin.gameplay;

import calebxzhou.rdi.service.IslandFileStorage;
import net.minecraft.world.level.chunk.storage.IOWorker;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.file.Path;

/**
 * calebxzhou @ 2025-09-03 14:16
 */
@Mixin(IOWorker.class)
public class mIslandStorage {
 /*   @Redirect(method = "<init>",at= @At(value = "NEW", target = "(Lnet/minecraft/world/level/chunk/storage/RegionStorageInfo;Ljava/nio/file/Path;Z)Lnet/minecraft/world/level/chunk/storage/RegionFileStorage;"))
    private RegionFileStorage RDI$Store(RegionStorageInfo info, Path folder, boolean sync){

        return new IslandFileStorage(info,folder,sync);
    }*/
}
