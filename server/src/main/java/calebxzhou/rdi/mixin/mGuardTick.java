package calebxzhou.rdi.mixin;

import calebxzhou.rdi.RDI;
import calebxzhou.rdi.service.EntityTicker;
import calebxzhou.rdi.service.RMobSpawner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.LevelTicks;
import net.minecraft.world.ticks.SavedTick;
import net.minecraft.world.ticks.ScheduledTick;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * calebxzhou @ 2024-06-05 9:51
 */
public class mGuardTick {
}


@Mixin(MinecraftServer.class)
abstract
class mTickInvertServer {
    @Shadow
    @Final
    private List<Runnable> tickables;

    @Shadow
    public abstract void tickChildren(BooleanSupplier hasTimeLeft);


    @Redirect(method = "tickServer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;tickChildren(Ljava/util/function/BooleanSupplier;)V"))
    private void tickServerChildrenNoCrash(MinecraftServer instance, BooleanSupplier bs) {
        try {
            tickChildren(bs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Redirect(method = "tickChildren", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tick(Ljava/util/function/BooleanSupplier;)V"))
    private void RDI$OnTickLevel(ServerLevel serverlevel, BooleanSupplier hasTimeLeft) {
        try {
                serverlevel.tick(hasTimeLeft);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
@Mixin(ServerChunkCache.class)
class mTickInvertMobSpawn{
    @Redirect(method = "tickChunks",at= @At(value = "INVOKE", target = "Lnet/minecraft/world/level/NaturalSpawner;spawnForChunk(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/NaturalSpawner$SpawnState;ZZZ)V"))
    private void RDI$spawnMobForChunk(ServerLevel lvl, LevelChunk chunk, NaturalSpawner.SpawnState state, boolean spawnFriendlies, boolean spawnMonsters, boolean forceDespawn){
        RMobSpawner.spawnForChunk(lvl,chunk,state,spawnFriendlies,spawnMonsters,forceDespawn);
    }
}
@Mixin(Level.class)
class mTickingEntity {
    @Redirect(method = "guardEntityTick", at = @At(value = "INVOKE", target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"))
    private <T extends Entity> void RDI$GuardEntityTick(Consumer<T> entityConsumer, Object t) {
        EntityTicker.tick(entityConsumer, (T) t);
    }
   /* @Overwrite
    public <T extends Entity> void guardEntityTick(Consumer<T> consumerEntity, T entity) {
        EntityTicker.tick(consumerEntity, entity);
    }*/
}

@Mixin(ServerLevel.class)
abstract
class mGuardServerLevelTick extends Level{

    private mGuardServerLevelTick(WritableLevelData levelData, ResourceKey<Level> dimension, RegistryAccess registryAccess, Holder<DimensionType> dimensionTypeRegistration, Supplier<ProfilerFiller> profiler, boolean isClientSide, boolean isDebug, long biomeZoomSeed, int maxChainedNeighborUpdates) {
        super(levelData, dimension, registryAccess, dimensionTypeRegistration, profiler, isClientSide, isDebug, biomeZoomSeed, maxChainedNeighborUpdates);
    }


/*    @Inject(method = "tick",at=@At("HEAD"), cancellable = true)
    private void tick$RDI(BooleanSupplier hasTimeLeft, CallbackInfo ci){
        //没人不tick
        if(players.isEmpty())
            ci.cancel();
    }*/

    @Shadow @Final private List<ServerPlayer> players;

    @Redirect(method = "tickBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private void tickBlock(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, RandomSource randomSource) {
        try {
                blockState.tick(serverLevel, blockPos, serverLevel.random);
        } catch (Exception e) {
            serverLevel.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 0);
            e.printStackTrace();
        }

    }

    /*@Redirect(method = "tickFluid", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    private void tickFluid(FluidState fluidState, Level level, BlockPos blockPos) {
        try {
            fluidState.tick(level, blockPos);
        } catch (Exception e) {
            level.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 0);
            e.printStackTrace();
        }

    }*/
    @Unique
    private int tick$=0;
    @Redirect(method = "tick",at= @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tickBlockEntities()V"))
    private void RDI$tickBlockEntties(ServerLevel level){
        if(players.isEmpty())
            return;
        try {
                this.tickBlockEntities();


            /*boolean hasActivePlayers = PlayerService.atLeast1NoAfk(players);
            if (hasActivePlayers) {*/
               /**/
           /* } else {
                if(tick$>=20){
                    this.tickBlockEntities();
                    tick$=0;
                }else{
                    tick$++;
                }
            }*/
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

@Mixin(ServerChunkCache.class)
class mGuardChunkTick {
    @Shadow
    @Final
    private DistanceManager distanceManager;
    @Shadow
    @Final
    public ChunkMap chunkMap;

    @Redirect(method = "runDistanceManagerUpdates()Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/DistanceManager;runAllUpdates(Lnet/minecraft/server/level/ChunkMap;)Z"))
    private boolean nocrashTick(DistanceManager instance, ChunkMap chunkStorage) {
        //return true;
        try {
            return this.distanceManager.runAllUpdates(this.chunkMap);
        } catch (Exception t) {
            t.printStackTrace();
        }
        return true;
    }

}

@Mixin(LevelTicks.class)
abstract
class mGuardLevelTick {
    @Shadow
    protected abstract void collectTicks(long gameTime, int maxAllowedTicks, ProfilerFiller profiler);

    @Shadow
    protected abstract void cleanupAfterTick();

    @Shadow
    protected abstract void runCollectedTicks(BiConsumer ticker);

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/ticks/LevelTicks;collectTicks(JILnet/minecraft/util/profiling/ProfilerFiller;)V"))
    private void guardCollectTicks(LevelTicks instance, long gameTime, int maxAllowedTicks, ProfilerFiller profiler) {
        try {
            collectTicks(gameTime, maxAllowedTicks, profiler);
        } catch (Exception e) {
            e.printStackTrace();
            cleanupAfterTick();
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/ticks/LevelTicks;runCollectedTicks(Ljava/util/function/BiConsumer;)V"))
    private void runCollectTicks(LevelTicks instance, BiConsumer ticker) {
        try {
            runCollectedTicks(ticker);
        } catch (Exception e) {
            e.printStackTrace();
            cleanupAfterTick();
        }
    }
}

@Mixin(LevelChunkTicks.class)
abstract
class mGuardLevelTick2 {
    @Mutable
    @Shadow
    @Final
    private Set<ScheduledTick<?>> ticksPerPosition;

    @Shadow
    protected abstract void scheduleUnchecked(ScheduledTick tick);

    @Mutable
    @Shadow
    @Final
    private Queue<ScheduledTick> tickQueue;

    @Shadow
    private List<SavedTick> pendingTicks;

    /*@Overwrite
    public void schedule(ScheduledTick tick) {
        try {
            if(ticksPerPosition == null){
                ticksPerPosition = new ObjectOpenCustomHashSet<>(ScheduledTick.UNIQUE_TICK_HASH);
            }
            if(tickQueue == null){
                tickQueue =  new PriorityQueue(ScheduledTick.DRAIN_ORDER);
            }
            if(pendingTicks == null){
                pendingTicks = new ArrayList<>();
            }
            if (this.ticksPerPosition.add(tick)) {
                this.scheduleUnchecked(tick);
            }
        } catch (Exception e) {
            RDI.log().warn(e.getLocalizedMessage());
            ticksPerPosition = new ObjectOpenCustomHashSet<>(ScheduledTick.UNIQUE_TICK_HASH);
            tickQueue =  new PriorityQueue(ScheduledTick.DRAIN_ORDER);
            pendingTicks = new ArrayList<>();
        }
    }*/
}
@Mixin(Animal.class)
class mAnimalAi{
    //卡顿时停止动物ai
    @Inject(method = "aiStep", at = @At(value = "HEAD"), cancellable = true)
    public void RDI$StopAiStep(CallbackInfo ci) {
        if(RDI.isLagging())
            ci.cancel();
    }
}