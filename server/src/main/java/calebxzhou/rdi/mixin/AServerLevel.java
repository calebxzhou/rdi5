package calebxzhou.rdi.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerLevel.class)
public interface AServerLevel {
    @Invoker
    void invokeTickPassenger(Entity ridingEntity, Entity passengerEntity);
    @Accessor
    ServerLevelData getServerLevelData();
    @Accessor
    PersistentEntitySectionManager<Entity> getEntityManager();
}
