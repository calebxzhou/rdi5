package calebxzhou.rdi.mixin;

import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(MinecraftServer.class)
public interface AMcServer {
    @Accessor
    PlayerDataStorage getPlayerDataStorage();
    @Accessor
    Map<ResourceKey<Level>, ServerLevel> getLevels();
    @Accessor
    LevelStorageSource.LevelStorageAccess getStorageSource();
    @Invoker
    ServerStatus invokeBuildServerStatus();
    @Accessor
    LayeredRegistryAccess<RegistryLayer> getRegistries();
}
