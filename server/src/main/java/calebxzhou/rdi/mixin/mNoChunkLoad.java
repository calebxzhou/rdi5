package calebxzhou.rdi.mixin;

import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.world.chunk.ForcedChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * calebxzhou @ 8/21/2025 10:57 AM
 */
@Mixin(ForcedChunkManager.class)
public class mNoChunkLoad {
    //拒绝区块加载器
    @Redirect(method = "forceChunk(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/server/level/TicketType;Lnet/neoforged/neoforge/common/world/chunk/ForcedChunkManager$TicketOwner;ZZ)V",
    at= @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerChunkCache;addRegionTicket(Lnet/minecraft/server/level/TicketType;Lnet/minecraft/world/level/ChunkPos;ILjava/lang/Object;Z)V") )
    private static void RDI$NOChunkLoad(ServerChunkCache instance, TicketType type, ChunkPos pos, int distance, Object value, boolean forceTicks){

    }
}
