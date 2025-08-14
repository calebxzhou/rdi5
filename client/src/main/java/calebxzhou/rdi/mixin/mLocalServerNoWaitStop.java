package calebxzhou.rdi.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * calebxzhou @ 2025-08-14 10:48
 */
@Mixin(MinecraftServer.class)
public class mLocalServerNoWaitStop {
    /*@Redirect(method = "waitForTasks",at= @At(value = "INVOKE", target = "Lnet/minecraft/util/thread/ReentrantBlockableEventLoop;waitForTasks()V"))
    private void RDI$NoWaitStop(ReentrantBlockableEventLoop instance) {
        // Do nothing, just skip the wait
    }*/
}
