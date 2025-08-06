package calebxzhou.rdi.mixin.gameplay;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * calebxzhou @ 2024-05-26 10:28
 */
@Mixin(MinecraftServer.class)
public abstract class mDifficulty {
    @Shadow public abstract void setDifficulty(Difficulty difficulty, boolean bl);

    @Inject(method = "loadLevel",at= @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;prepareLevels(Lnet/minecraft/server/level/progress/ChunkProgressListener;)V"))
    private void setHard(CallbackInfo ci){
        setDifficulty(Difficulty.HARD,true);
    }
}
